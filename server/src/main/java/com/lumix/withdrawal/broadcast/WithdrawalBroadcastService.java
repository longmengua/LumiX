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
        if(rows.size()!=1) throw new IllegalArgumentException("signed withdrawal does not exist"); Map<String,Object> row=rows.getFirst(); WithdrawalStatus status=WithdrawalStatus.valueOf((String)row.get("status")); if(status!=WithdrawalStatus.SIGNED&&status!=WithdrawalStatus.BROADCASTING&&status!=WithdrawalStatus.BROADCAST_UNKNOWN) return;
        String txHash=(String)row.get("signed_transaction_hash"), raw=(String)row.get("signed_raw_transaction"); if(txHash==null||raw==null) throw new IllegalStateException("signed transaction evidence is incomplete");
        if(status==WithdrawalStatus.SIGNED){states.requireTransition(status,WithdrawalStatus.BROADCASTING);jdbc.update("UPDATE withdrawals SET status='BROADCASTING',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='SIGNED'",withdrawalId);}
        // 在任何可能送出 RPC 前先留下 txHash；timeout 後只能以這個 hash 對帳／重送同一 raw tx。
        persistAttempt(withdrawalId, txHash);
        boolean known;
        try { known=receiptExists(txHash); }
        catch (RuntimeException exception) { markUnknown(withdrawalId,row,"receipt lookup unavailable before broadcast"); return; }
        String returned=txHash;
        if(!known) { try { returned=rpc.sendRawTransaction(raw); } catch(IOException exception) { metrics.rpcError(); markUnknown(withdrawalId,row,"RPC response unavailable after raw transaction submission"); return; } }
        if(returned!=null&&!returned.equalsIgnoreCase(txHash)) throw new IllegalStateException("RPC transaction hash differs from signer hash");
        jdbc.update("UPDATE withdrawal_nonce_allocations SET status='BROADCASTED',transaction_hash=?,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",txHash,withdrawalId);
        WithdrawalStatus transitionFrom=status==WithdrawalStatus.BROADCAST_UNKNOWN ? WithdrawalStatus.BROADCAST_UNKNOWN : WithdrawalStatus.BROADCASTING;
        states.requireTransition(transitionFrom,WithdrawalStatus.BROADCASTED); if(jdbc.update("UPDATE withdrawals SET status='BROADCASTED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",withdrawalId,transitionFrom.name())!=1) throw new IllegalStateException("broadcast state race");
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('SYSTEM','ethereum-broadcast','WITHDRAWAL_BROADCASTED','WITHDRAWAL',? ,?,'SUCCESS','BROADCASTED')",withdrawalId,row.get("request_id"));
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('WITHDRAWAL',?,'WithdrawalBroadcasted',?,'READY',?)",withdrawalId,"txHash="+txHash,row.get("request_id")); metrics.broadcast();
    }
    private boolean receiptExists(String hash){try{return rpc.transactionReceipt(hash).isPresent();}catch(IOException exception){throw new IllegalStateException("chain receipt cannot be observed safely",exception);}}
    private void persistAttempt(String withdrawalId,String txHash) {
        jdbc.update("INSERT INTO withdrawal_chain_transactions(id,withdrawal_id,transaction_hash,raw_transaction_hash,broadcast_attempts) VALUES (?,?,?,?,1) ON CONFLICT(withdrawal_id) DO UPDATE SET broadcast_attempts=withdrawal_chain_transactions.broadcast_attempts+1,updated_at=CURRENT_TIMESTAMP",java.util.UUID.randomUUID(),withdrawalId,txHash,txHash.substring(2));
        jdbc.update("UPDATE withdrawal_nonce_allocations SET status='SIGNED',transaction_hash=?,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='RESERVED'",txHash,withdrawalId);
    }
    /** 不確定不代表失敗；hold 保持鎖定，後續 worker 先查 tx hash 再考慮重送同一 raw tx。 */
    private void markUnknown(String withdrawalId,Map<String,Object> row,String reason) {
        WithdrawalStatus current=WithdrawalStatus.valueOf((String)row.get("status"));
        if (current==WithdrawalStatus.BROADCASTING || current==WithdrawalStatus.BROADCAST_UNKNOWN) {
            if(current==WithdrawalStatus.BROADCASTING) states.requireTransition(current,WithdrawalStatus.BROADCAST_UNKNOWN);
            jdbc.update("UPDATE withdrawals SET status='BROADCAST_UNKNOWN',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",withdrawalId,current.name());
            jdbc.update("UPDATE withdrawal_nonce_allocations SET status='BROADCAST_UNKNOWN',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",withdrawalId);
            jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,reason,after_state) VALUES ('SYSTEM','ethereum-broadcast','WITHDRAWAL_BROADCAST_UNKNOWN','WITHDRAWAL',? ,?,'UNKNOWN',?,'BROADCAST_UNKNOWN')",withdrawalId,row.get("request_id"),reason);
        }
    }
}
