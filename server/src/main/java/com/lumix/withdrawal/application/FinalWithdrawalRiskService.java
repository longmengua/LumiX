package com.lumix.withdrawal.application;

import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import com.lumix.withdrawal.review.WithdrawalReviewService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 簽章前最後一次 fail-closed 檢查。
 *
 * <p>這個 gate 重讀鎖定中的 database state，不重用 create 當時的記憶體結果；任何 mismatch 都轉入
 * RISK_HOLD，絕不把已批准當成仍可簽章的證明。</p>
 */
@Service
public class FinalWithdrawalRiskService {
    private final JdbcTemplate jdbc; private final WithdrawalStateMachine states = new WithdrawalStateMachine();
    public FinalWithdrawalRiskService(JdbcTemplate jdbc) { this.jdbc=Objects.requireNonNull(jdbc); }
    @Transactional
    public boolean advance(String withdrawalId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT w.withdrawal_id,w.user_id,w.asset_symbol,w.network,w.chain_id,w.amount,w.fee_amount,w.address,w.token_contract,w.status,w.approval_payload_hash,w.reservation_id,w.request_id,u.withdrawal_frozen_at,u.fund_transfer_restricted_until,r.status reservation_status,a.status account_status,ast.status asset_status,wa.status address_status FROM withdrawals w JOIN users u ON u.user_id=w.user_id JOIN reservations r ON r.reservation_id=w.reservation_id JOIN accounts a ON a.account_id=w.account_id JOIN assets ast ON ast.asset_symbol=w.asset_symbol LEFT JOIN withdrawal_addresses wa ON wa.id=w.destination_address_id WHERE w.withdrawal_id=? FOR UPDATE",withdrawalId);
        if(rows.size()!=1) throw new IllegalArgumentException("withdrawal does not exist"); Map<String,Object> row=rows.getFirst();
        if(!"APPROVED".equals(row.get("status"))) return false;
        states.requireTransition(WithdrawalStatus.APPROVED,WithdrawalStatus.FINAL_RISK_CHECK);
        jdbc.update("UPDATE withdrawals SET status='FINAL_RISK_CHECK',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='APPROVED'",withdrawalId);
        // restriction 到期後不應永久卡住提款；未到期或資料型別不明則保守地拒絕送簽。
        boolean transferRestrictionActive = isFuture(row.get("fund_transfer_restricted_until"));
        boolean safe=row.get("withdrawal_frozen_at")==null && !transferRestrictionActive && "ACTIVE".equals(row.get("reservation_status")) && "ACTIVE".equals(row.get("account_status")) && "ACTIVE".equals(row.get("asset_status")) && "ACTIVE".equals(row.get("address_status")) && row.get("approval_payload_hash")!=null && WithdrawalReviewService.approvalHash(row).equals(row.get("approval_payload_hash"));
        WithdrawalStatus target=safe?WithdrawalStatus.SIGN_PENDING:WithdrawalStatus.RISK_HOLD;
        states.requireTransition(WithdrawalStatus.FINAL_RISK_CHECK,target);
        jdbc.update("UPDATE withdrawals SET status=?,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='FINAL_RISK_CHECK'",target.name(),withdrawalId);
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,reason,after_state) VALUES ('SYSTEM','withdrawal-final-risk',?,'WITHDRAWAL',?,?,?, ?,?)",safe?"WITHDRAWAL_FINAL_RISK_PASSED":"WITHDRAWAL_FINAL_RISK_REJECTED",withdrawalId,row.get("request_id"),safe?"SUCCESS":"REJECTED",safe?"all current controls valid":"current state or approval hash mismatch",target.name());
        return safe;
    }
    private static boolean isFuture(Object value) {
        if (value == null) return false;
        if (!(value instanceof java.sql.Timestamp timestamp)) return true;
        return timestamp.toInstant().isAfter(Instant.now());
    }
}
