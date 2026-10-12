package com.lumix.withdrawal.broadcast;

import com.lumix.ethereum.rpc.EthereumRpcClient;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import com.lumix.withdrawal.application.WithdrawalMetrics;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 主 server 的唯一鏈上廣播邊界。
 *
 * <p>RPC timeout 不會造成重簽：先以 signer 已派生的 tx hash 查 receipt，只有未觀測到時才重送完全相同
 * raw transaction。BROADCASTING 不是失敗或可釋放資金的狀態。</p>
 */
@Service @ConditionalOnBean(EthereumRpcClient.class)
public class WithdrawalBroadcastService {
    private final JdbcTemplate jdbc; private final EthereumRpcClient rpc; private final WithdrawalMetrics metrics; private final WithdrawalStateMachine states=new WithdrawalStateMachine();
    public WithdrawalBroadcastService(JdbcTemplate jdbc,EthereumRpcClient rpc,WithdrawalMetrics metrics){this.jdbc=Objects.requireNonNull(jdbc);this.rpc=Objects.requireNonNull(rpc);this.metrics=Objects.requireNonNull(metrics);}
    @Transactional public void broadcast(String withdrawalId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT w.withdrawal_id,w.request_id,w.status,s.signed_transaction_hash,s.signed_raw_transaction FROM withdrawals w JOIN withdrawal_signing_requests s ON s.withdrawal_id=w.withdrawal_id WHERE w.withdrawal_id=? FOR UPDATE",withdrawalId);
        if(rows.size()!=1) throw new IllegalArgumentException("signed withdrawal does not exist"); Map<String,Object> row=rows.getFirst(); WithdrawalStatus status=WithdrawalStatus.valueOf((String)row.get("status")); if(status!=WithdrawalStatus.SIGNED&&status!=WithdrawalStatus.BROADCASTING) return;
        String txHash=(String)row.get("signed_transaction_hash"), raw=(String)row.get("signed_raw_transaction"); if(txHash==null||raw==null) throw new IllegalStateException("signed transaction evidence is incomplete");
        if(status==WithdrawalStatus.SIGNED){states.requireTransition(status,WithdrawalStatus.BROADCASTING);jdbc.update("UPDATE withdrawals SET status='BROADCASTING',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='SIGNED'",withdrawalId);}
        boolean known=receiptExists(txHash); String returned=txHash;
        if(!known) { try { returned=rpc.sendRawTransaction(raw); } catch(IOException exception) { metrics.rpcError(); if(!receiptExists(txHash)) throw new IllegalStateException("broadcast result remains ambiguous; retry same raw transaction only",exception); } }
        if(returned!=null&&!returned.equalsIgnoreCase(txHash)) throw new IllegalStateException("RPC transaction hash differs from signer hash");
        jdbc.update("INSERT INTO withdrawal_chain_transactions(id,withdrawal_id,transaction_hash,raw_transaction_hash,broadcast_attempts,broadcasted_at) VALUES (?,?,?,?,1,CURRENT_TIMESTAMP) ON CONFLICT(withdrawal_id) DO UPDATE SET broadcast_attempts=withdrawal_chain_transactions.broadcast_attempts+1,broadcasted_at=COALESCE(withdrawal_chain_transactions.broadcasted_at,CURRENT_TIMESTAMP),updated_at=CURRENT_TIMESTAMP",java.util.UUID.randomUUID(),withdrawalId,txHash,txHash.substring(2));
        states.requireTransition(WithdrawalStatus.BROADCASTING,WithdrawalStatus.BROADCASTED); if(jdbc.update("UPDATE withdrawals SET status='BROADCASTED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='BROADCASTING'",withdrawalId)!=1) throw new IllegalStateException("broadcast state race");
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('SYSTEM','ethereum-broadcast','WITHDRAWAL_BROADCASTED','WITHDRAWAL',? ,?,'SUCCESS','BROADCASTED')",withdrawalId,row.get("request_id"));
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('WITHDRAWAL',?,'WithdrawalBroadcasted',?,'READY',?)",withdrawalId,"txHash="+txHash,row.get("request_id")); metrics.broadcast();
    }
    private boolean receiptExists(String hash){try{return rpc.transactionReceipt(hash).isPresent();}catch(IOException exception){throw new IllegalStateException("chain receipt cannot be observed safely",exception);}}
}
