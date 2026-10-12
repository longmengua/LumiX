package com.lumix.withdrawal.review;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端 review API；所有 command 由 WithdrawalReviewService 再驗證 server-side role。 */
@RestController @Profile("infrastructure") @RequestMapping("/api/admin/v1/withdrawals")
public class AdminWithdrawalReviewController {
    private final WithdrawalReviewService reviews; private final JdbcTemplate jdbc; private final SuperAdminAccessService access;
    public AdminWithdrawalReviewController(WithdrawalReviewService reviews,JdbcTemplate jdbc,SuperAdminAccessService access) { this.reviews=Objects.requireNonNull(reviews);this.jdbc=Objects.requireNonNull(jdbc);this.access=Objects.requireNonNull(access); }
    @GetMapping("/review-queue") public java.util.List<java.util.Map<String,Object>> queue(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) { access.requireActiveSuperAdmin(actor); java.util.List<java.util.Map<String,Object>> result=jdbc.queryForList("SELECT withdrawal_id,user_id,asset_symbol,amount,address,status,created_at FROM withdrawals WHERE status IN ('MANUAL_REVIEW','RISK_HOLD') ORDER BY created_at"); jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,outcome) VALUES ('ADMIN',?,'WITHDRAWAL_REVIEW_QUEUE_VIEW','WITHDRAWAL','review-queue','SUCCESS')",actor.userId()); return result; }
    @GetMapping("/{id}") public java.util.Map<String,Object> detail(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,@PathVariable String id) { access.requireActiveSuperAdmin(actor); java.util.Map<String,Object> result=jdbc.queryForMap("SELECT withdrawal_id,user_id,asset_symbol,network,amount,fee_amount,address,status,approval_payload_hash,created_at FROM withdrawals WHERE withdrawal_id=?",id); jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,outcome) VALUES ('ADMIN',?,'WITHDRAWAL_REVIEW_VIEW','WITHDRAWAL',?,'SUCCESS')",actor.userId(),id); return result; }
    @PostMapping("/{id}/approve") public ResponseEntity<Void> approve(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,@PathVariable String id,@RequestBody Decision body) { reviews.approve(actor,id,body.reason()); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/reject") public ResponseEntity<Void> reject(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,@PathVariable String id,@RequestBody Decision body) { reviews.reject(actor,id,body.reason()); return ResponseEntity.noContent().build(); }
    public record Decision(String reason) { }
}
