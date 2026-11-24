package com.lumix.trading.liquidity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Demo provider 由資料庫 runtime 開關控制；模擬結果留在記憶體，絕不寫入真實訂單或 ledger。 */
@Component
public class DemoExternalOrderRouter implements ExternalOrderRouter {
    public enum Scenario { NORMAL, DELAYED, PARTIAL, SLIPPAGE_FILLED, SLIPPAGE_REJECTED, VENUE_REJECTED, UNKNOWN_TIMEOUT }

    private record Simulated(Intent intent, Outcome outcome, Instant availableAt, Outcome completed) { }
    private final Map<String, Simulated> outcomes = new ConcurrentHashMap<>();

    @Override public String source() { return "DEMO"; }

    /** 隨機選擇使用者體驗情境；延遲訂單先回 PENDING，後續 lookup 回報成交。 */
    @Override public Outcome submit(Intent intent) {
        validate(intent);
        return submit(intent, Scenario.values()[ThreadLocalRandom.current().nextInt(Scenario.values().length)]);
    }

    /** 依原 clientOrderId 查模擬結果；不存在時回 UNKNOWN，不捏造已成交。 */
    @Override public Outcome lookup(String source, String clientOrderId) {
        if (!source().equals(source) || clientOrderId == null || clientOrderId.length() > 96)
            throw new IllegalArgumentException("demo lookup key is invalid");
        Simulated stored = outcomes.get(clientOrderId);
        if (stored == null) return empty(clientOrderId, null, Status.UNKNOWN, Reason.ORDER_NOT_FOUND);
        if (stored.availableAt() != null && Instant.now().isAfter(stored.availableAt())) {
            outcomes.replace(clientOrderId, stored,
                    new Simulated(stored.intent(), stored.completed(), null, stored.completed()));
            return stored.completed();
        }
        return stored.outcome();
    }

    /** 供受控 demo caller 指定情境，讓 UI/流程驗收可重現；一般 submit 仍隨機選情境。 */
    public Outcome submit(Intent intent, Scenario scenario) {
        validate(intent);
        if (scenario == null) throw new IllegalArgumentException("demo scenario is required");
        Simulated value = outcomes.compute(intent.clientOrderId(), (key, existing) -> {
            if (existing != null && !existing.intent().equals(intent))
                throw new IllegalArgumentException("clientOrderId conflicts with another demo intent");
            return existing == null ? simulate(intent, scenario) : existing;
        });
        return value.outcome();
    }

    private Simulated simulate(Intent intent, Scenario scenario) {
        String externalId = "demo-" + intent.clientOrderId();
        BigDecimal quantity = intent.quantity();
        return switch (scenario) {
            case NORMAL -> done(intent, filled(intent, externalId, quantity, intent.referencePrice(),
                    Status.FILLED, Reason.FILLED_AT_REFERENCE));
            case DELAYED -> {
                Outcome pending = empty(intent.clientOrderId(), externalId, Status.PENDING, Reason.VENUE_ACK_DELAYED);
                Outcome complete = filled(intent, externalId, quantity, intent.referencePrice(),
                        Status.FILLED, Reason.DELAYED_FILL_CONFIRMED);
                yield new Simulated(intent, pending, Instant.now().plusSeconds(3), complete);
            }
            case PARTIAL -> {
                BigDecimal partial = quantity.divide(new BigDecimal("2"), 8, RoundingMode.DOWN);
                yield done(intent, partial.signum() == 0
                        ? empty(intent.clientOrderId(), externalId, Status.CANCELED, Reason.QUANTITY_TOO_SMALL)
                        : filled(intent, externalId, partial, intent.referencePrice(),
                                Status.PARTIALLY_FILLED, Reason.PARTIAL_LIQUIDITY));
            }
            case SLIPPAGE_FILLED -> {
                BigDecimal moved = intent.referencePrice().multiply("BUY".equals(intent.side().name())
                        ? new BigDecimal("1.003") : new BigDecimal("0.997"));
                BigDecimal fillPrice = "BUY".equals(intent.side().name())
                        ? moved.min(intent.limitPrice()) : moved.max(intent.limitPrice());
                yield done(intent, filled(intent, externalId, quantity, fillPrice,
                        Status.FILLED, Reason.FILLED_WITHIN_SLIPPAGE_LIMIT));
            }
            case SLIPPAGE_REJECTED -> done(intent,
                    empty(intent.clientOrderId(), externalId, Status.REJECTED, Reason.SLIPPAGE_LIMIT_EXCEEDED));
            case VENUE_REJECTED -> done(intent,
                    empty(intent.clientOrderId(), externalId, Status.REJECTED, Reason.VENUE_REJECTED_ORDER));
            case UNKNOWN_TIMEOUT -> done(intent,
                    empty(intent.clientOrderId(), externalId, Status.UNKNOWN, Reason.VENUE_OUTCOME_UNKNOWN));
        };
    }

    private static Simulated done(Intent intent, Outcome outcome) {
        return new Simulated(intent, outcome, null, outcome);
    }

    private static Outcome filled(Intent intent, String externalId, BigDecimal quantity, BigDecimal price,
                                  Status status, Reason reason) {
        return new Outcome(intent.clientOrderId(), externalId, status, status.code(), quantity,
                quantity.multiply(price), price, reason, reason.code());
    }

    private static Outcome empty(String clientOrderId, String externalId, Status status, Reason reason) {
        return new Outcome(clientOrderId, externalId, status, status.code(), BigDecimal.ZERO,
                BigDecimal.ZERO, null, reason, reason.code());
    }

    private static void validate(Intent intent) {
        if (intent == null || intent.clientOrderId() == null
                || !intent.clientOrderId().matches("[A-Za-z0-9_-]{1,96}") || intent.kind() == null
                || intent.marketSymbol() == null || !intent.marketSymbol().matches("[A-Z0-9][A-Z0-9_-]{0,31}")
                || intent.side() == null || intent.quantity() == null || intent.quantity().signum() <= 0
                || intent.referencePrice() == null || intent.referencePrice().signum() <= 0
                || intent.limitPrice() == null || intent.limitPrice().signum() <= 0 || intent.timeInForce() == null
                || (intent.side() == Side.BUY && intent.limitPrice().compareTo(intent.referencePrice()) < 0)
                || (intent.side() == Side.SELL && intent.limitPrice().compareTo(intent.referencePrice()) > 0))
            throw new IllegalArgumentException("demo external order intent is invalid");
    }
}
