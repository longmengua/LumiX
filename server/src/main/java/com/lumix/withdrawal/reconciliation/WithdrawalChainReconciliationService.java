package com.lumix.withdrawal.reconciliation;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ethereum.rpc.EthereumBlock;
import com.lumix.ethereum.rpc.EthereumRpcClient;
import com.lumix.ethereum.rpc.EthereumTransactionReceipt;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.runtime.LedgerPostingActor;
import com.lumix.ledger.runtime.LedgerPostingExecutionCommand;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.reservation.ReservationCaptureService;
import com.lumix.reservation.ReservationReleaseService;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import com.lumix.withdrawal.application.WithdrawalMetrics;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** receipt、canonical block 與 finalized checkpoint 都成立後才做 immutable ledger settlement。 */
@Service @ConditionalOnBean(EthereumRpcClient.class)
public class WithdrawalChainReconciliationService {
    private static final String CUSTODY_ACCOUNT="system:withdrawal-custody:spot";
    private final JdbcTemplate jdbc; private final EthereumRpcClient rpc; private final ReservationCaptureService captures; private final ReservationReleaseService releases; private final TransactionalLedgerPostingService ledger; private final WithdrawalMetrics metrics; private final WithdrawalStateMachine states=new WithdrawalStateMachine();
    public WithdrawalChainReconciliationService(JdbcTemplate jdbc,EthereumRpcClient rpc,ReservationCaptureService captures,ReservationReleaseService releases,TransactionalLedgerPostingService ledger,WithdrawalMetrics metrics){this.jdbc=Objects.requireNonNull(jdbc);this.rpc=Objects.requireNonNull(rpc);this.captures=Objects.requireNonNull(captures);this.releases=Objects.requireNonNull(releases);this.ledger=Objects.requireNonNull(ledger);this.metrics=Objects.requireNonNull(metrics);}
    @Transactional public void reconcile(String withdrawalId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT w.withdrawal_id,w.request_id,w.account_id,w.asset_symbol,w.amount,w.reservation_id,w.status,c.transaction_hash FROM withdrawals w JOIN withdrawal_chain_transactions c ON c.withdrawal_id=w.withdrawal_id WHERE w.withdrawal_id=? FOR UPDATE",withdrawalId); if(rows.size()!=1)return; Map<String,Object> row=rows.getFirst(); WithdrawalStatus current=WithdrawalStatus.valueOf((String)row.get("status")); if(current!=WithdrawalStatus.BROADCAST_UNKNOWN&&current!=WithdrawalStatus.BROADCASTED&&current!=WithdrawalStatus.CONFIRMING)return;
        EthereumTransactionReceipt receipt; try { receipt=rpc.transactionReceipt((String)row.get("transaction_hash")).orElse(null); } catch(IOException exception){metrics.rpcError();throw new IllegalStateException("chain receipt is unavailable",exception);} if(receipt==null){
            // UNKNOWN 尚未證明任何 node 接受過交易；只能由 broadcast worker 查詢後重送同一 raw tx，不能移出它的工作集合。
            if (current == WithdrawalStatus.BROADCASTED) moveToConfirming(withdrawalId,current);
            return;
        }
        jdbc.update("UPDATE withdrawal_chain_transactions SET receipt_status=?,block_number=?,gas_used=?,effective_gas_price=?,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",receipt.success()?"SUCCESS":"FAILED",receipt.blockNumber(),receipt.gasUsed(),receipt.effectiveGasPrice(),withdrawalId);
        if(!receipt.success()){failAfterBroadcast(row,"receipt failed: principal transfer did not complete; gas may have been consumed");return;}
        EthereumBlock canonical; EthereumBlock finalized; try{canonical=rpc.blockByNumber(receipt.blockNumber());finalized=rpc.finalizedBlock();}catch(IOException exception){metrics.rpcError();throw new IllegalStateException("finality evidence unavailable",exception);} if(!receipt.blockHash().equalsIgnoreCase(canonical.hash())||receipt.blockNumber().compareTo(finalized.number())>0){moveToConfirming(withdrawalId,current);return;}
        settle(row,receipt.blockNumber());
    }
    private void moveToConfirming(String id,WithdrawalStatus current){if(current==WithdrawalStatus.BROADCASTED||current==WithdrawalStatus.BROADCAST_UNKNOWN){states.requireTransition(current,WithdrawalStatus.CONFIRMING);jdbc.update("UPDATE withdrawals SET status='CONFIRMING',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",id,current.name());}}
    /** 已確定 failed receipt 才能釋放 principal hold；gas 證據已寫入鏈交易表，不能將它誤認為完全未發生。 */
    private void failAfterBroadcast(Map<String,Object> row,String reason){WithdrawalStatus current=WithdrawalStatus.valueOf((String)row.get("status")); states.requireTransition(current,WithdrawalStatus.FAILED); if(jdbc.update("UPDATE withdrawals SET status='FAILED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",row.get("withdrawal_id"),current.name())!=1) return; releases.release((String)row.get("reservation_id"),(String)row.get("request_id"),"withdrawal-chain-failure-release:"+row.get("withdrawal_id"),"chain-reconciliation");jdbc.update("UPDATE withdrawal_nonce_allocations SET status='CHAIN_FAILED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",row.get("withdrawal_id"));jdbc.update("UPDATE withdrawal_chain_transactions SET chain_failure_recorded_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",row.get("withdrawal_id"));jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,reason,after_state) VALUES ('SYSTEM','chain-reconciliation','WITHDRAWAL_CHAIN_FAILED','WITHDRAWAL',? ,?,'FAILED',?,'FAILED')",row.get("withdrawal_id"),row.get("request_id"),reason);metrics.failed();}
    private void settle(Map<String,Object> row,java.math.BigInteger blockNumber){String id=(String)row.get("withdrawal_id"),asset=(String)row.get("asset_symbol");BigDecimal amount=(BigDecimal)row.get("amount");ensureCustodyAsset(asset);captures.capture((String)row.get("reservation_id"),(String)row.get("request_id"),"withdrawal-capture:"+id,"chain-reconciliation");ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId((String)row.get("request_id")),new LedgerJournalDraft(LedgerBusinessReferenceType.WITHDRAWAL,id,List.of(new LedgerEntryDraft(new AccountId((String)row.get("account_id")),new AssetSymbol(asset),LedgerDirection.DEBIT,amount,1),new LedgerEntryDraft(new AccountId(CUSTODY_ACCOUNT),new AssetSymbol(asset),LedgerDirection.CREDIT,amount,2))),Instant.now()),"withdrawal-ledger:"+id,new LedgerPostingActor("SYSTEM","chain-reconciliation"))); WithdrawalStatus current=WithdrawalStatus.valueOf((String)row.get("status")); if(current==WithdrawalStatus.BROADCASTED||current==WithdrawalStatus.BROADCAST_UNKNOWN){states.requireTransition(current,WithdrawalStatus.CONFIRMING);jdbc.update("UPDATE withdrawals SET status='CONFIRMING' WHERE withdrawal_id=? AND status=?",id,current.name());} states.requireTransition(WithdrawalStatus.CONFIRMING,WithdrawalStatus.COMPLETED);jdbc.update("UPDATE withdrawals SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='CONFIRMING'",id);jdbc.update("UPDATE withdrawal_nonce_allocations SET status='SETTLED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",id);jdbc.update("UPDATE withdrawal_chain_transactions SET finalized_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=?",id);jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('SYSTEM','chain-reconciliation','WITHDRAWAL_COMPLETED','WITHDRAWAL',? ,?,'SUCCESS','COMPLETED')",id,row.get("request_id"));jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('WITHDRAWAL',?,'WithdrawalCompleted',?,'READY',?)",id,"finalizedBlock="+blockNumber,row.get("request_id"));metrics.completed();}
    private void ensureCustodyAsset(String asset){jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,?,'ACTIVE') ON CONFLICT(account_id,asset_symbol) DO NOTHING",CUSTODY_ACCOUNT,asset);}
}
