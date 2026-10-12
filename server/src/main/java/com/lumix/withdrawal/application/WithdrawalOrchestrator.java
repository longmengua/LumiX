package com.lumix.withdrawal.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.reservation.ReservationHoldCommand;
import com.lumix.reservation.ReservationHoldService;
import com.lumix.reservation.ReservationReleaseService;
import com.lumix.withdrawal.address.WithdrawalAddressBookService;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import com.lumix.withdrawal.risk.WithdrawalVelocityService;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import com.lumix.withdrawal.risk.RiskDecision;
import com.lumix.withdrawal.risk.WithdrawalRiskContext;
import com.lumix.withdrawal.risk.WithdrawalRiskEngine;
import com.lumix.withdrawal.review.WithdrawalReviewService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 提款 application orchestrator。
 *
 * <p>它只協調 address、reservation 與 risk；不包含 SQL raw signing、Ethereum RPC 或 review 授權邏輯。
 * create transaction 內的 claim、主表 snapshot 與 hold 必須一起成功或一起回滾。</p>
 */
@Service
public class WithdrawalOrchestrator {
    private final JdbcTemplate jdbc; private final WithdrawalAddressBookService addresses; private final ReservationHoldService holds; private final ReservationReleaseService releases; private final WithdrawalMetrics metrics;
    private final WithdrawalRiskEngine risk; private final WithdrawalFeePolicy fees; private final WithdrawalPlatformProperties properties; private final ObjectProvider<WithdrawalVelocityService> velocity; private final WithdrawalStateMachine states=new WithdrawalStateMachine(); private final Clock clock;
    public WithdrawalOrchestrator(JdbcTemplate jdbc,WithdrawalAddressBookService addresses,ReservationHoldService holds,ReservationReleaseService releases,WithdrawalRiskEngine risk,WithdrawalFeePolicy fees,WithdrawalPlatformProperties properties,ObjectProvider<WithdrawalVelocityService> velocity,WithdrawalMetrics metrics) { this(jdbc,addresses,holds,releases,risk,fees,properties,velocity,metrics,Clock.systemUTC()); }
    WithdrawalOrchestrator(JdbcTemplate jdbc,WithdrawalAddressBookService addresses,ReservationHoldService holds,ReservationReleaseService releases,WithdrawalRiskEngine risk,WithdrawalFeePolicy fees,WithdrawalPlatformProperties properties,ObjectProvider<WithdrawalVelocityService> velocity,WithdrawalMetrics metrics,Clock clock) { this.jdbc=Objects.requireNonNull(jdbc);this.addresses=Objects.requireNonNull(addresses);this.holds=Objects.requireNonNull(holds);this.releases=Objects.requireNonNull(releases);this.risk=Objects.requireNonNull(risk);this.fees=Objects.requireNonNull(fees);this.properties=Objects.requireNonNull(properties);this.velocity=Objects.requireNonNull(velocity);this.metrics=Objects.requireNonNull(metrics);this.clock=Objects.requireNonNull(clock); }
    @Transactional
    public Result create(Command command) {
        validate(command); String fingerprint=hash(command.userId()+"|"+command.asset()+"|ETHEREUM|"+command.amount().toPlainString()+"|"+command.destinationAddressId());
        if(jdbc.update("INSERT INTO idempotency_keys(scope,idempotency_key,request_id,status,response_summary) VALUES ('WITHDRAWAL_CREATE',?,?, 'IN_PROGRESS',?) ON CONFLICT(scope,idempotency_key) DO NOTHING",command.idempotencyKey(),command.requestId(),fingerprint)==0) return replay(command,fingerprint);
        WithdrawalAddressBookService.Address address=addresses.requireActive(command.userId(),command.destinationAddressId(),command.asset());
        String account=sourceAccount(command.userId(),command.asset()); WithdrawalFeePolicy.Quote quote=fees.quote(command.asset(),"ETHEREUM",command.amount());
        String withdrawalId="wd_"+UUID.randomUUID(); String reservationId="withdrawal:"+withdrawalId; Instant now=Instant.now(clock);
        jdbc.update("INSERT INTO withdrawals(withdrawal_id,request_id,account_id,asset_symbol,chain_type,address,amount,fee_amount,status,user_id,network,destination_address_id,normalized_address,chain_id,token_contract,reservation_id,created_ip,created_device_id,request_fingerprint) VALUES (?,?,?,?, 'ERC20',?,?,?,'REQUESTED',?,?,?, ?,11155111,?, ?,?,?,?)",withdrawalId,command.requestId(),account,command.asset(),address.address(),command.amount(),quote.platformFee(),command.userId(),"ETHEREUM",UUID.fromString(address.id()),address.normalizedAddress(),tokenContract(command.asset()),reservationId,command.ip(),command.deviceId(),fingerprint);
        transition(withdrawalId,WithdrawalStatus.REQUESTED,WithdrawalStatus.FUNDS_HELD);
        try { holds.hold(new ReservationHoldCommand(reservationId,account,command.asset(),withdrawalId,command.amount().add(quote.platformFee()),command.requestId(),"withdrawal-hold:"+withdrawalId,command.userId(),"WITHDRAWAL","WITHDRAWAL_HOLD")); }
        catch(IllegalStateException exception) { throw new ApiException(ApiErrorCode.INSUFFICIENT_USER_BALANCE,exception,null); }
        audit(command.userId(),"WITHDRAWAL_FUNDS_HELD",withdrawalId,WithdrawalStatus.FUNDS_HELD.name(),command.requestId());
        transition(withdrawalId,WithdrawalStatus.FUNDS_HELD,WithdrawalStatus.RISK_PENDING);
        WithdrawalRiskEngine.Assessment assessment=risk.evaluate(securityContext(command)); persistRisk(withdrawalId,assessment);
        WithdrawalStatus next=switch(assessment.decision()) { case ALLOW -> WithdrawalStatus.APPROVED; case MANUAL_REVIEW -> WithdrawalStatus.MANUAL_REVIEW; case HOLD -> WithdrawalStatus.RISK_HOLD; case DENY -> WithdrawalStatus.REJECTED; };
        switch (assessment.decision()) { case ALLOW -> metrics.riskAllow(); case MANUAL_REVIEW, HOLD -> metrics.riskReview(); case DENY -> metrics.riskDeny(); }
        // 自動核准也必須固定一份 payload hash，否則 final risk 無從驗證 TOCTOU。
        if (next == WithdrawalStatus.APPROVED) {
            jdbc.update("UPDATE withdrawals SET approval_payload_hash=? WHERE withdrawal_id=?", approvalHash(withdrawalId), withdrawalId);
        }
        transition(withdrawalId,WithdrawalStatus.RISK_PENDING,next); audit(command.userId(),"WITHDRAWAL_RISK_EVALUATED",withdrawalId,next.name(),command.requestId());
        // 拒絕發生在廣播前，保留 hold 會不當凍結使用者資金；release 本身具有 durable idempotency。
        if (next == WithdrawalStatus.REJECTED) {
            releases.release(reservationId, command.requestId(), "withdrawal-release:" + withdrawalId, "withdrawal-risk");
        }
        // transactional outbox 只留下事件證據；不在資金 transaction 內直接呼叫 Kafka 或任一外部 broker。
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('WITHDRAWAL',?,'WithdrawalRiskDecisionMade',?,'READY',?)",withdrawalId,"decision="+assessment.decision().name(),command.requestId());
        // Redis failure 不可使已持久化提款 rollback 成不存在；後續 risk evaluation 會直接進人工審核。
        WithdrawalVelocityService velocityService=velocity.getIfAvailable();
        if(velocityService!=null) { try { velocityService.record(command.userId(),command.deviceId()==null?null:command.deviceId().toString(),command.ip(),command.amount()); } catch (WithdrawalVelocityService.VelocityUnavailableException ignored) { metrics.velocityRedisError(); } }
        jdbc.update("UPDATE idempotency_keys SET status='COMPLETED',resource_type='WITHDRAWAL',resource_id=?,updated_at=CURRENT_TIMESTAMP WHERE scope='WITHDRAWAL_CREATE' AND idempotency_key=? AND status='IN_PROGRESS'",withdrawalId,command.idempotencyKey());
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('WITHDRAWAL',?,'WithdrawalRequested',?,'READY',?)",withdrawalId,"status="+next.name(),command.requestId());
        audit(command.userId(),"WITHDRAWAL_REQUESTED",withdrawalId,next.name(),command.requestId()); metrics.requested(); return new Result(withdrawalId,next,command.amount(),command.asset(),address.address(),now,false);
    }
    private WithdrawalRiskContext securityContext(Command c) { List<Map<String,Object>> rows=jdbc.queryForList("SELECT u.withdrawal_frozen_at,u.fund_transfer_restricted_until,uc.password_changed_at, EXISTS(SELECT 1 FROM user_login_devices d WHERE d.user_id=u.user_id AND d.revoked_at IS NULL AND d.last_ip_address=?) trusted, EXISTS(SELECT 1 FROM user_sessions s WHERE s.user_id=u.user_id AND s.revoked_at IS NULL AND s.ip_address=?) known_ip FROM users u JOIN user_credentials uc ON uc.user_id=u.user_id WHERE u.user_id=?",c.ip(),c.ip(),c.userId()); if(rows.size()!=1) throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR); Map<String,Object> r=rows.getFirst(); return new WithdrawalRiskContext(c.userId(),c.amount(),asInstant(r.get("password_changed_at")),asInstant(r.get("fund_transfer_restricted_until")),asInstant(r.get("withdrawal_frozen_at")),Boolean.TRUE.equals(r.get("trusted")),!Boolean.TRUE.equals(r.get("known_ip")),c.ip(),c.deviceId()==null?null:c.deviceId().toString()); }
    private String approvalHash(String withdrawalId) { List<Map<String,Object>> rows=jdbc.queryForList("SELECT withdrawal_id,user_id,asset_symbol,network,chain_id,amount,fee_amount,address,token_contract FROM withdrawals WHERE withdrawal_id=?", withdrawalId); if(rows.size()!=1) throw new IllegalStateException("withdrawal snapshot missing"); return WithdrawalReviewService.approvalHash(rows.getFirst()); }
    private void persistRisk(String id,WithdrawalRiskEngine.Assessment a) { UUID assessment=UUID.randomUUID(); jdbc.update("INSERT INTO withdrawal_risk_assessments(id,withdrawal_id,decision,score,evaluated_at,engine_version) VALUES (?,?,?, ?,CURRENT_TIMESTAMP,'v1')",assessment,id,a.decision().name(),a.score()); a.triggers().forEach(t->jdbc.update("INSERT INTO withdrawal_risk_triggers(id,assessment_id,rule_code,score_delta,decision_effect,evidence_json) VALUES (?,?,?,?,?,CAST(? AS jsonb))",UUID.randomUUID(),assessment,t.ruleCode(),t.scoreDelta(),t.decision().name(),"{\"evaluated\":true}")); }
    private void transition(String id,WithdrawalStatus from,WithdrawalStatus to) { states.requireTransition(from,to); if(jdbc.update("UPDATE withdrawals SET status=?,updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",to.name(),id,from.name())!=1) throw new IllegalStateException("withdrawal state transition lost concurrency race"); }
    private String sourceAccount(String user,String asset) { List<String> rows=jdbc.query("SELECT a.account_id FROM accounts a JOIN account_assets aa ON aa.account_id=a.account_id WHERE a.user_id=? AND a.account_type='SPOT' AND a.status='ACTIVE' AND aa.asset_symbol=? AND aa.status='ACTIVE'",(rs,n)->rs.getString(1),user,asset); if(rows.size()!=1) throw new ApiException(ApiErrorCode.INSUFFICIENT_USER_BALANCE); return rows.getFirst(); }
    private Result replay(Command c,String fingerprint) { List<Map<String,Object>> rows=jdbc.queryForList("SELECT w.withdrawal_id,w.status,w.amount,w.asset_symbol,w.address,w.created_at,k.response_summary FROM idempotency_keys k JOIN withdrawals w ON w.withdrawal_id=k.resource_id WHERE k.scope='WITHDRAWAL_CREATE' AND k.idempotency_key=?",c.idempotencyKey()); if(rows.size()!=1 || !fingerprint.equals(rows.getFirst().get("response_summary"))) throw new ApiException(ApiErrorCode.IDEMPOTENCY_CONFLICT); Map<String,Object> r=rows.getFirst(); return new Result((String)r.get("withdrawal_id"),WithdrawalStatus.valueOf((String)r.get("status")),(BigDecimal)r.get("amount"),(String)r.get("asset_symbol"),(String)r.get("address"),asInstant(r.get("created_at")),true); }
    private void audit(String actor,String action,String id,String after,String requestId) { jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('USER',?,?,'WITHDRAWAL',?,?, 'SUCCESS',?)",actor,action,id,requestId,after); }
    /** Sepolia test token 必須由部署明確設定，禁止把 Mainnet USDT contract 混到測試網。 */
    private String tokenContract(String asset) { if(!"USDT".equals(asset)) return null; String configured=properties.getSepoliaUsdtContract(); if(configured==null||configured.isBlank()) throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE); return com.lumix.withdrawal.address.EthereumWithdrawalAddress.normalize(configured); }
    private static Instant asInstant(Object v) { return v==null?null:((java.sql.Timestamp)v).toInstant(); }
    private static String hash(String input) { try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);} }
    private static void validate(Command c) { Objects.requireNonNull(c); if(!List.of("ETH","USDT").contains(c.asset())||c.amount()==null||c.amount().signum()<=0||c.amount().scale()>18) throw new ApiException(ApiErrorCode.VALIDATION_ERROR); }
    /** device id 只接受由 HttpOnly device cookie 解析出的 UUID，不能相信 request body 或 header。 */
    public record Command(String userId,String asset,BigDecimal amount,String destinationAddressId,String idempotencyKey,String requestId,String ip,UUID deviceId) { }
    public record Result(String withdrawalId,WithdrawalStatus status,BigDecimal amount,String asset,String destination,Instant createdAt,boolean replayed) { }
}
