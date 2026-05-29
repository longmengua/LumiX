/*
 * 檔案用途：Hedge venue adapter decorator，以 refId 防止外部 effectful submit 重複送單。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class IdempotentHedgeVenueAdapter implements HedgeVenueAdapter {

    private final HedgeVenueAdapter delegate;
    private final Map<String, Record> records = new ConcurrentHashMap<>();

    public IdempotentHedgeVenueAdapter(@Qualifier("rejectingHedgeVenueAdapter") HedgeVenueAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public HedgeOrderResult submit(HedgeOrderRequest request) {
        String key = key(request);
        String fingerprint = fingerprint(request);
        Record existing = records.get(key);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                return HedgeOrderResult.rejected("HEDGE_VENUE_IDEMPOTENCY_CONFLICT");
            }
            if (existing.result().retryable()) {
                return HedgeOrderResult.retryableRejected("HEDGE_VENUE_OUTCOME_UNCERTAIN");
            }
            return existing.result();
        }

        HedgeOrderResult result = delegate.submit(request);
        Record newRecord = new Record(fingerprint, result);
        Record raced = records.putIfAbsent(key, newRecord);
        if (raced == null) {
            return result;
        }
        if (!raced.fingerprint().equals(fingerprint)) {
            return HedgeOrderResult.rejected("HEDGE_VENUE_IDEMPOTENCY_CONFLICT");
        }
        return raced.result().retryable()
                ? HedgeOrderResult.retryableRejected("HEDGE_VENUE_OUTCOME_UNCERTAIN")
                : raced.result();
    }

    private static String key(HedgeOrderRequest request) {
        if (request == null || request.refId() == null || request.refId().isBlank()) {
            throw new IllegalArgumentException("hedge venue idempotency requires refId");
        }
        return request.refId().trim();
    }

    private static String fingerprint(HedgeOrderRequest request) {
        return String.join("|",
                normalize(request.marketMakerId()),
                Long.toString(request.uid()),
                normalize(request.symbol()),
                request.side() == null ? "" : request.side().name(),
                normalizeDecimal(request.quantity()),
                normalizeDecimal(request.referencePrice()),
                normalizeDecimal(request.limitPrice())
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String normalizeDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private record Record(String fingerprint, HedgeOrderResult result) {
    }
}
