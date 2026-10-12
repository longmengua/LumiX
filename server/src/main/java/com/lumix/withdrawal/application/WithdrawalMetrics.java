package com.lumix.withdrawal.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 提款 runtime 的低基數 metrics 邊界。
 *
 * <p>只以固定名稱與無使用者／地址 label 的 counter/gauge 呈現，避免 metrics backend 被高基數金融識別資料
 * 汙染。財務事實仍以 PostgreSQL ledger/reservation 為準。</p>
 */
@Component
public class WithdrawalMetrics {
    private final Counter requested;
    private final Counter riskAllow;
    private final Counter riskReview;
    private final Counter riskDeny;
    private final Counter signed;
    private final Counter broadcast;
    private final Counter completed;
    private final Counter failed;
    private final Counter velocityRedisError;
    private final Counter signerError;
    private final Counter rpcError;

    public WithdrawalMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
        Objects.requireNonNull(registry);
        Objects.requireNonNull(jdbc);
        requested = counter(registry, "withdrawal.requested.total");
        riskAllow = counter(registry, "withdrawal.risk.allow.total");
        riskReview = counter(registry, "withdrawal.risk.review.total");
        riskDeny = counter(registry, "withdrawal.risk.deny.total");
        signed = counter(registry, "withdrawal.signed.total");
        broadcast = counter(registry, "withdrawal.broadcast.total");
        completed = counter(registry, "withdrawal.completed.total");
        failed = counter(registry, "withdrawal.failed.total");
        velocityRedisError = counter(registry, "withdrawal.velocity.redis.error.total");
        signerError = counter(registry, "withdrawal.signer.error.total");
        rpcError = counter(registry, "withdrawal.rpc.error.total");
        gauge(registry, jdbc, "withdrawal.review.pending", "status IN ('MANUAL_REVIEW','RISK_HOLD')");
        gauge(registry, jdbc, "withdrawal.sign.pending", "status IN ('SIGN_PENDING','SIGNING')");
        gauge(registry, jdbc, "withdrawal.chain.pending", "status IN ('BROADCASTED','CONFIRMING')");
    }

    public void requested() { requested.increment(); }
    public void riskAllow() { riskAllow.increment(); }
    public void riskReview() { riskReview.increment(); }
    public void riskDeny() { riskDeny.increment(); }
    public void signed() { signed.increment(); }
    public void broadcast() { broadcast.increment(); }
    public void completed() { completed.increment(); }
    public void failed() { failed.increment(); }
    public void velocityRedisError() { velocityRedisError.increment(); }
    public void signerError() { signerError.increment(); }
    public void rpcError() { rpcError.increment(); }

    private static Counter counter(MeterRegistry registry, String name) {
        return Counter.builder(name).description("LumiX withdrawal runtime evidence").register(registry);
    }

    private static void gauge(MeterRegistry registry, JdbcTemplate jdbc, String name, String predicate) {
        Gauge.builder(name, jdbc, template -> template.queryForObject("SELECT count(*) FROM withdrawals WHERE " + predicate, Integer.class))
            .description("目前處於提款 runtime gate 的筆數")
            .register(registry);
    }
}
