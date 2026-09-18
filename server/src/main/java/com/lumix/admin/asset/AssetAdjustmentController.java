package com.lumix.admin.asset;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端通用資產修正 API；effectful command 強制 Idempotency-Key。 */
@RestController @Profile("infrastructure") @RequestMapping("/api/admin/v1/assets/adjustments")
public class AssetAdjustmentController {
    private final AssetAdjustmentService service; private final JdbcTemplate jdbc;
    public AssetAdjustmentController(AssetAdjustmentService service, JdbcTemplate jdbc) { this.service=service; this.jdbc=jdbc; }
    @PostMapping public ResponseEntity<Response> create(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor, @RequestHeader("Idempotency-Key") String key, @RequestBody Request r) {
        AssetAdjustmentResult value=service.execute(actor,new AssetAdjustmentCommand(r.adjustmentType(),r.userId(),r.accountType(),r.assetSymbol(),r.direction(),r.amount(),r.sourceBusinessType(),r.sourceBusinessId(),r.sourceJournalId(),r.sourceLedgerEntryId(),r.incidentReference(),r.reason(),key));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").body(new Response(Long.toString(value.adjustmentId()),Long.toString(value.ledgerJournalId()),value.replayed())); }
    @GetMapping public List<Summary> list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) { service.requireAuthorized(actor); return jdbc.query("SELECT adjustment_id,adjustment_type,direction,user_id,account_type,asset_symbol,amount,source_business_type,source_business_id,source_journal_id,source_ledger_entry_id,incident_reference,ledger_journal_id,actor_id,reason,status,created_at FROM admin_asset_adjustments ORDER BY created_at DESC,adjustment_id DESC LIMIT 100",(rs,n)->new Summary(Long.toString(rs.getLong(1)),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getBigDecimal(7).stripTrailingZeros().toPlainString(),rs.getString(8),rs.getString(9),text(rs.getObject(10)),text(rs.getObject(11)),rs.getString(12),Long.toString(rs.getLong(13)),rs.getString(14),rs.getString(15),rs.getString(16),rs.getTimestamp(17).toInstant())); }
    @GetMapping("/{adjustmentId}") public Summary detail(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,@PathVariable long adjustmentId) { return list(actor).stream().filter(v->v.adjustmentId().equals(Long.toString(adjustmentId))).findFirst().orElseThrow(()->new AssetAdjustmentException(ApiErrorCode.ADJUSTMENT_NOT_FOUND)); }
    @GetMapping("/reversal-sources/{ledgerEntryId}") public AssetAdjustmentReversalSource source(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,@PathVariable long ledgerEntryId) { return service.lookupReversalSource(actor,ledgerEntryId); }
    private static String text(Object value){return value==null?null:value.toString();}
    public record Request(AssetAdjustmentType adjustmentType,String userId,String accountType,String assetSymbol,AssetAdjustmentDirection direction,BigDecimal amount,String sourceBusinessType,String sourceBusinessId,Long sourceJournalId,Long sourceLedgerEntryId,String incidentReference,String reason) { }
    public record Response(String adjustmentId,String ledgerJournalId,boolean replayed) { }
    public record Summary(String adjustmentId,String adjustmentType,String direction,String userId,String accountType,String assetSymbol,String amount,String sourceBusinessType,String sourceBusinessId,String sourceJournalId,String sourceLedgerEntryId,String incidentReference,String ledgerJournalId,String actorId,String reason,String status,java.time.Instant createdAt) { }
}
