package com.lumix.withdrawal.review;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.reservation.ReservationReleaseService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import com.lumix.withdrawal.application.FinalWithdrawalRiskService;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端人工審核 runtime。
 *
 * <p>授權一律由 server-side super-admin gate 驗證，request owner 不得核准自己提款；拒絕在同一交易釋放 hold。
 * 高額提款必須由兩名不同 super-admin 留下核准證據；第一位 reviewer 後仍維持 MANUAL_REVIEW。</p>
 */
@Service
public class WithdrawalReviewService {
    private final SuperAdminAccessService access; private final JdbcTemplate jdbc; private final ReservationReleaseService releases; private final FinalWithdrawalRiskService finalRisk; private final WithdrawalPlatformProperties properties; private final WithdrawalStateMachine states=new WithdrawalStateMachine();
    public WithdrawalReviewService(SuperAdminAccessService access,JdbcTemplate jdbc,ReservationReleaseService releases,FinalWithdrawalRiskService finalRisk,WithdrawalPlatformProperties properties) { this.access=Objects.requireNonNull(access);this.jdbc=Objects.requireNonNull(jdbc);this.releases=Objects.requireNonNull(releases);this.finalRisk=Objects.requireNonNull(finalRisk);this.properties=Objects.requireNonNull(properties); }
    @Transactional public void approve(AuthenticatedUser reviewer,String id,String reason) {
        access.requireActiveSuperAdmin(reviewer); Map<String,Object> w=lock(id); if(reviewer.userId().equals(w.get("user_id"))) throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        WithdrawalStatus from=WithdrawalStatus.valueOf((String)w.get("status")); if(from!=WithdrawalStatus.MANUAL_REVIEW && from!=WithdrawalStatus.RISK_HOLD) throw new ApiException(ApiErrorCode.CONFLICT);
        String payload=approvalHash(w); jdbc.update("INSERT INTO withdrawal_reviews(id,withdrawal_id,reviewer_user_id,reviewer_role,decision,reason,approval_payload_hash) VALUES (?,?,?,'SUPER_ADMIN','APPROVE',?,?)",UUID.randomUUID(),id,reviewer.userId(),requiredReason(reason),payload);
        boolean dualReviewRequired=((BigDecimal)w.get("amount")).compareTo(properties.getDualReviewAmount())>=0;
        int approvals=jdbc.queryForObject("SELECT count(*) FROM withdrawal_reviews WHERE withdrawal_id=? AND decision='APPROVE' AND approval_payload_hash=?",Integer.class,id,payload);
        if (dualReviewRequired && approvals < 2) { audit(reviewer.userId(),"WITHDRAWAL_REVIEW_APPROVED",id,"MANUAL_REVIEW",reason); return; }
        states.requireTransition(from,WithdrawalStatus.APPROVED); jdbc.update("UPDATE withdrawals SET status='APPROVED',approval_payload_hash=?,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",payload,id,from.name()); audit(reviewer.userId(),"WITHDRAWAL_REVIEW_APPROVED",id,"APPROVED",reason);
        // approval transaction 尚未 commit 時 final risk 仍使用同一鎖定 transaction，避免核准後被置換資料再送簽。
        finalRisk.advance(id);
    }
    @Transactional public void reject(AuthenticatedUser reviewer,String id,String reason) {
        access.requireActiveSuperAdmin(reviewer); Map<String,Object> w=lock(id); if(reviewer.userId().equals(w.get("user_id"))) throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        WithdrawalStatus from=WithdrawalStatus.valueOf((String)w.get("status")); if(from!=WithdrawalStatus.MANUAL_REVIEW && from!=WithdrawalStatus.RISK_HOLD) throw new ApiException(ApiErrorCode.CONFLICT);
        String hash=approvalHash(w); jdbc.update("INSERT INTO withdrawal_reviews(id,withdrawal_id,reviewer_user_id,reviewer_role,decision,reason,approval_payload_hash) VALUES (?,?,?,'SUPER_ADMIN','REJECT',?,?)",UUID.randomUUID(),id,reviewer.userId(),requiredReason(reason),hash);
        states.requireTransition(from,WithdrawalStatus.REJECTED); jdbc.update("UPDATE withdrawals SET status='REJECTED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",id,from.name()); releases.release((String)w.get("reservation_id"),(String)w.get("request_id"),"withdrawal-release:"+id,"system"); audit(reviewer.userId(),"WITHDRAWAL_REVIEW_REJECTED",id,"REJECTED",reason);
    }
    private Map<String,Object> lock(String id) { List<Map<String,Object>> r=jdbc.queryForList("SELECT withdrawal_id,user_id,asset_symbol,network,chain_id,amount,fee_amount,address,token_contract,status,reservation_id,request_id FROM withdrawals WHERE withdrawal_id=? FOR UPDATE",id); if(r.size()!=1) throw new ApiException(ApiErrorCode.NOT_FOUND); return r.getFirst(); }
    /** canonical field order 是 TOCTOU 防線；sign 前的 final risk 必須使用完全相同的編碼重新計算。 */
    public static String approvalHash(Map<String,Object> w) { String text=w.get("withdrawal_id")+"\n"+w.get("user_id")+"\n"+w.get("asset_symbol")+"\n"+w.get("network")+"\n"+w.get("chain_id")+"\n"+((BigDecimal)w.get("amount")).toPlainString()+"\n"+((BigDecimal)w.get("fee_amount")).toPlainString()+"\n"+w.get("address")+"\n"+String.valueOf(w.get("token_contract")); try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);} }
    private static String requiredReason(String value) { if(value==null||value.trim().isEmpty()||value.trim().length()>512) throw new ApiException(ApiErrorCode.VALIDATION_ERROR);return value.trim(); }
    private void audit(String actor,String action,String id,String after,String reason) { jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,outcome,reason,after_state) VALUES ('ADMIN',? ,?,'WITHDRAWAL',?,'SUCCESS',?,?)",actor,action,id,reason,after); }
}
