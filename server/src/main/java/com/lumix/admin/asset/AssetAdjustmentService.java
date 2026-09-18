package com.lumix.admin.asset;

import com.lumix.account.AccountId;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.account.AssetSymbol;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.runtime.LedgerPostingActor;
import com.lumix.ledger.runtime.LedgerPostingExecutionCommand;
import com.lumix.ledger.runtime.LedgerPostingExecutionResult;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 受治理的通用資產修正 command boundary。
 *
 * <p>這裡故意不修改 historical ledger 或 balance projection。所有效果交給同交易的 posting service append，
 * 而 aggregate 只保存 command、來源與稽核 reference；因此來源不存在的人工異常也是一等資料模型。</p>
 */
@Service
public class AssetAdjustmentService {
    private static final String COUNTERPARTY = "system:asset-adjustment:spot";
    private final SuperAdminAccessService access;
    private final JdbcTemplate jdbc;
    private final TransactionalLedgerPostingService ledger;

    public AssetAdjustmentService(SuperAdminAccessService access, JdbcTemplate jdbc, TransactionalLedgerPostingService ledger) {
        this.access = Objects.requireNonNull(access); this.jdbc = Objects.requireNonNull(jdbc); this.ledger = Objects.requireNonNull(ledger);
    }

    /** 同一 transaction 包住 caller idempotency、領域驗證、ledger、aggregate、admin audit。 */
    @Transactional
    public AssetAdjustmentResult execute(AuthenticatedUser actor, AssetAdjustmentCommand raw) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor));
        AssetAdjustmentCommand command = validate(raw);
        String key = digest(command.idempotencyKey());
        String fingerprint = fingerprint(actor, command);
        if (!claim(key, fingerprint)) return replay(key, fingerprint);

        Resolved resolved = command.adjustmentType() == AssetAdjustmentType.BUSINESS_REVERSAL
                ? resolveReversal(command) : resolveDirect(command);
        ensureCounterparty(resolved.assetSymbol());
        if (resolved.direction() == AssetAdjustmentDirection.DEBIT) ensureSufficientUserBalance(resolved.accountId(), resolved.assetSymbol(), command.amount());

        List<LedgerEntryDraft> entries = resolved.direction() == AssetAdjustmentDirection.CREDIT
                ? List.of(entry(COUNTERPARTY, resolved.assetSymbol(), LedgerDirection.DEBIT, command.amount(), 1), entry(resolved.accountId(), resolved.assetSymbol(), LedgerDirection.CREDIT, command.amount(), 2))
                : List.of(entry(resolved.accountId(), resolved.assetSymbol(), LedgerDirection.DEBIT, command.amount(), 1), entry(COUNTERPARTY, resolved.assetSymbol(), LedgerDirection.CREDIT, command.amount(), 2));
        String businessId = "asset-adjustment:" + key;
        LedgerPostingExecutionResult posted = ledger.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId("asset-adj-" + key.substring(0, 40)), new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT, businessId, entries), Instant.now()),
                "asset-adjustment-ledger:" + key, new LedgerPostingActor("ADMIN", actor.userId())));
        if (posted.replayed()) throw new IllegalStateException("ledger idempotency state is inconsistent");
        Long adjustmentId = jdbc.queryForObject("INSERT INTO admin_asset_adjustments (adjustment_type,direction,user_id,account_id,account_type,asset_symbol,amount,source_business_type,source_business_id,source_journal_id,source_ledger_entry_id,incident_reference,ledger_journal_id,actor_id,reason) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING adjustment_id",
                Long.class, command.adjustmentType().name(), resolved.direction().name(), resolved.userId(), resolved.accountId(), resolved.accountType(), resolved.assetSymbol(), command.amount(), command.sourceBusinessType(), command.sourceBusinessId(), resolved.sourceJournalId(), resolved.sourceLedgerEntryId(), command.incidentReference(), posted.ledgerJournalId(), actor.userId(), command.reason());
        if (adjustmentId == null) throw new IllegalStateException("adjustment record was not created");
        jdbc.update("INSERT INTO audit_logs (actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,reason) VALUES ('ADMIN',?,'ADMIN_ASSET_ADJUSTMENT','ADMIN_ASSET_ADJUSTMENT',?,'asset-adj-' || ?,'SUCCESS',?)",
                actor.userId(), adjustmentId.toString(), key.substring(0, 40), auditReason(command, resolved, posted.ledgerJournalId()));
        if (jdbc.update("UPDATE idempotency_keys SET status='COMPLETED',resource_type='ADMIN_ASSET_ADJUSTMENT',resource_id=?,response_summary=?,updated_at=CURRENT_TIMESTAMP WHERE scope='ADMIN_ACTION' AND idempotency_key=? AND status='IN_PROGRESS'", adjustmentId.toString(), fingerprint, key) != 1) throw new IllegalStateException("adjustment idempotency completion failed");
        return new AssetAdjustmentResult(adjustmentId, posted.ledgerJournalId(), false);
    }

    @Transactional(readOnly = true)
    public AssetAdjustmentReversalSource lookupReversalSource(AuthenticatedUser actor, long entryId) {
        access.requireActiveSuperAdmin(actor);
        try { return source(entryId, false); } catch (IllegalArgumentException e) { return new AssetAdjustmentReversalSource(Long.toString(entryId), null, null, null, null, null, null, null, null, null, null, null, false, e.getMessage()); }
    }

    /** 唯讀 endpoints 也必須經過相同 server-side super-admin gate。 */
    public void requireAuthorized(AuthenticatedUser actor) { access.requireActiveSuperAdmin(Objects.requireNonNull(actor)); }

    private Resolved resolveDirect(AssetAdjustmentCommand c) {
        if (!"SPOT".equals(c.accountType())) throw new IllegalArgumentException("unsupported adjustment account type");
        String accountId = resolveUserAccount(c.userId(), c.accountType());
        ensureActiveAccountAsset(accountId, c.assetSymbol());
        return new Resolved(c.userId(), accountId, c.accountType(), c.assetSymbol(), c.direction(), c.sourceJournalId(), c.sourceLedgerEntryId());
    }
    private Resolved resolveReversal(AssetAdjustmentCommand c) {
        if (c.sourceLedgerEntryId() == null) throw new AssetAdjustmentException(ApiErrorCode.SOURCE_LEDGER_ENTRY_NOT_FOUND);
        AssetAdjustmentReversalSource source = source(c.sourceLedgerEntryId(), true);
        if (!source.eligible()) throw new AssetAdjustmentException(ApiErrorCode.SOURCE_LEDGER_ENTRY_NOT_ELIGIBLE);
        if (c.sourceBusinessType() != null && (!c.sourceBusinessType().equals(source.sourceBusinessType()) || !c.sourceBusinessId().equals(source.sourceBusinessId()))) throw new AssetAdjustmentException(ApiErrorCode.INVALID_BUSINESS_REFERENCE);
        if (new BigDecimal(source.remainingReversible()).compareTo(c.amount()) < 0) throw new AssetAdjustmentException(ApiErrorCode.REVERSAL_AMOUNT_EXCEEDS_REMAINING);
        AssetAdjustmentDirection direction = "CREDIT".equals(source.originalDirection()) ? AssetAdjustmentDirection.DEBIT : AssetAdjustmentDirection.CREDIT;
        return new Resolved(source.userId(), resolveUserAccount(source.userId(), source.accountType()), source.accountType(), source.assetSymbol(), direction, Long.valueOf(source.journalId()), Long.valueOf(source.ledgerEntryId()));
    }
    /** FOR UPDATE 鎖定原始 immutable entry，serializes all partial reversals of the same effect. */
    private AssetAdjustmentReversalSource source(long entryId, boolean lock) {
        String suffix = lock ? " FOR UPDATE" : "";
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT e.ledger_entry_id,e.ledger_journal_id,e.direction,e.amount,e.account_id,e.asset_symbol,j.business_reference_type,j.business_reference_id,a.user_id,a.account_type,a.account_category,u.email FROM ledger_entries e JOIN ledger_journals j ON j.ledger_journal_id=e.ledger_journal_id JOIN accounts a ON a.account_id=e.account_id JOIN users u ON u.user_id=a.user_id WHERE e.ledger_entry_id=?" + suffix, entryId);
        if (rows.size()!=1) throw new AssetAdjustmentException(ApiErrorCode.SOURCE_LEDGER_ENTRY_NOT_FOUND);
        Map<String,Object> r=rows.getFirst(); String ineligible=null;
        if (!"USER".equals(r.get("account_category"))) ineligible="source ledger entry is not a user asset effect";
        else if ("ADJUSTMENT".equals(r.get("business_reference_type"))) ineligible="adjustment ledger entries cannot be business reversal sources";
        BigDecimal reversed = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM admin_asset_adjustments WHERE adjustment_type='BUSINESS_REVERSAL' AND source_ledger_entry_id=?", BigDecimal.class, entryId);
        BigDecimal amount=(BigDecimal)r.get("amount"); BigDecimal remaining=amount.subtract(reversed == null ? BigDecimal.ZERO : reversed);
        return new AssetAdjustmentReversalSource(Long.toString(entryId), r.get("ledger_journal_id").toString(), (String)r.get("user_id"), (String)r.get("email"), (String)r.get("account_type"), (String)r.get("asset_symbol"), (String)r.get("direction"), plain(amount), plain(reversed), plain(remaining), (String)r.get("business_reference_type"), (String)r.get("business_reference_id"), ineligible==null, ineligible);
    }
    private boolean claim(String key, String fingerprint) { return jdbc.update("INSERT INTO idempotency_keys(scope,idempotency_key,request_id,status,response_summary) VALUES ('ADMIN_ACTION',?,'asset-adj-' || ?,'IN_PROGRESS',?) ON CONFLICT(scope,idempotency_key) DO NOTHING", key, key.substring(0,40), fingerprint)==1; }
    private AssetAdjustmentResult replay(String key,String fingerprint) { List<Map<String,Object>> r=jdbc.queryForList("SELECT resource_id,response_summary,status FROM idempotency_keys WHERE scope='ADMIN_ACTION' AND idempotency_key=?",key); if(r.size()!=1 || !fingerprint.equals(r.getFirst().get("response_summary"))) throw new AssetAdjustmentException(ApiErrorCode.IDEMPOTENCY_CONFLICT); if(!"COMPLETED".equals(r.getFirst().get("status"))) throw new IllegalStateException("idempotency key is not replayable"); long id=Long.parseLong(r.getFirst().get("resource_id").toString()); Long journal=jdbc.queryForObject("SELECT ledger_journal_id FROM admin_asset_adjustments WHERE adjustment_id=?",Long.class,id); if(journal==null) throw new IllegalStateException("idempotency adjustment is missing"); return new AssetAdjustmentResult(id,journal,true); }
    private void ensureSufficientUserBalance(String account,String asset,BigDecimal amount) { List<BigDecimal> rows=jdbc.query("SELECT available_amount FROM balance_projections WHERE account_id=? AND asset_symbol=? FOR UPDATE",(rs,n)->rs.getBigDecimal(1),account,asset); if(rows.size()!=1 || rows.getFirst().compareTo(amount)<0) throw new AssetAdjustmentException(ApiErrorCode.INSUFFICIENT_USER_BALANCE); }
    private String resolveUserAccount(String user,String type) { List<String> r=jdbc.query("SELECT account_id FROM accounts WHERE user_id=? AND account_type=? AND account_category='USER' AND status IN ('ACTIVE','FROZEN')",(rs,n)->rs.getString(1),user,type); if(r.size()!=1) throw new IllegalArgumentException("user account is not eligible"); return r.getFirst(); }
    private void ensureCounterparty(String asset) { List<Map<String,Object>> r=jdbc.queryForList("SELECT account_category,account_purpose,status FROM accounts WHERE account_id=?",COUNTERPARTY); if(r.size()!=1 || !"EXCHANGE".equals(r.getFirst().get("account_category")) || !"ASSET_ADJUSTMENT_COUNTERPARTY".equals(r.getFirst().get("account_purpose")) || !"ACTIVE".equals(r.getFirst().get("status"))) throw new AssetAdjustmentException(ApiErrorCode.INVALID_ACCOUNT_PURPOSE); ensureActiveAccountAsset(COUNTERPARTY,asset); }
    private void ensureActiveAccountAsset(String account,String asset) { Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM assets WHERE asset_symbol=? AND status='ACTIVE'",Integer.class,asset); if(active==null||active!=1) throw new IllegalArgumentException("asset is not active"); jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES(?,?,'ACTIVE') ON CONFLICT(account_id,asset_symbol) DO NOTHING",account,asset); Integer relation=jdbc.queryForObject("SELECT COUNT(*) FROM account_assets WHERE account_id=? AND asset_symbol=? AND status='ACTIVE'",Integer.class,account,asset); if(relation==null||relation!=1) throw new IllegalArgumentException("account asset is not active"); }
    private static LedgerEntryDraft entry(String a,String asset,LedgerDirection d,BigDecimal amount,long sequence) { return new LedgerEntryDraft(new AccountId(a),new AssetSymbol(asset),d,amount,sequence); }
    private static AssetAdjustmentCommand validate(AssetAdjustmentCommand c) { Objects.requireNonNull(c); if(c.adjustmentType()==null||c.amount()==null||c.amount().signum()<=0) throw new AssetAdjustmentException(ApiErrorCode.INVALID_ADJUSTMENT_AMOUNT); if(blank(c.reason(),256)==null||blank(c.idempotencyKey(),128)==null) throw new IllegalArgumentException("reason and idempotency key are required"); String bt=optional(c.sourceBusinessType(),32),bi=optional(c.sourceBusinessId(),128); if((bt==null)!=(bi==null)) throw new AssetAdjustmentException(ApiErrorCode.INVALID_BUSINESS_REFERENCE); if(c.adjustmentType()!=AssetAdjustmentType.BUSINESS_REVERSAL && (c.userId()==null||c.accountType()==null||c.assetSymbol()==null||c.direction()==null)) throw new IllegalArgumentException("user, account, asset and direction are required"); return new AssetAdjustmentCommand(c.adjustmentType(),optional(c.userId(),64),optional(c.accountType(),16),optional(c.assetSymbol(),32),c.direction(),c.amount().stripTrailingZeros(),bt,bi,c.sourceJournalId(),c.sourceLedgerEntryId(),optional(c.incidentReference(),128),blank(c.reason(),256),blank(c.idempotencyKey(),128)); }
    private static String optional(String s,int max){ if(s==null||s.trim().isEmpty())return null; return blank(s,max); } private static String blank(String s,int max){ if(s==null)return null; s=s.trim(); if(s.isEmpty()||s.length()>max)throw new IllegalArgumentException("invalid adjustment text field"); return s; }
    private static String fingerprint(AuthenticatedUser a,AssetAdjustmentCommand c){ return digest(a.userId()+"|"+c.adjustmentType()+"|"+c.userId()+"|"+c.accountType()+"|"+c.assetSymbol()+"|"+c.direction()+"|"+c.amount().toPlainString()+"|"+c.sourceBusinessType()+"|"+c.sourceBusinessId()+"|"+c.sourceLedgerEntryId()+"|"+c.incidentReference()+"|"+c.reason()); }
    private static String digest(String v){ try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);} } private static String plain(BigDecimal v){return v==null?"0":v.stripTrailingZeros().toPlainString();}
    private static String auditReason(AssetAdjustmentCommand c,Resolved r,long journal){return "adjustmentId=ledger:"+journal+";type="+c.adjustmentType()+";direction="+r.direction()+";sourceBusinessType="+c.sourceBusinessType()+";sourceBusinessId="+c.sourceBusinessId()+";sourceLedgerEntryId="+r.sourceLedgerEntryId()+";incident="+c.incidentReference()+";reason="+c.reason();}
    private record Resolved(String userId,String accountId,String accountType,String assetSymbol,AssetAdjustmentDirection direction,Long sourceJournalId,Long sourceLedgerEntryId) { }
}
