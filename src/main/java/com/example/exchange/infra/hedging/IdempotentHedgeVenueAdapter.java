/*
 * 檔案用途：Hedge venue adapter decorator，以 refId 防止外部 effectful submit 重複送單。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyRecord;
import com.example.exchange.domain.repository.HedgeVenueIdempotencyStore;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Primary
public class IdempotentHedgeVenueAdapter implements HedgeVenueAdapter {

    private final HedgeVenueAdapter delegate;
    private final HedgeVenueIdempotencyStore store;

    public IdempotentHedgeVenueAdapter(@Qualifier("rejectingHedgeVenueAdapter") HedgeVenueAdapter delegate) {
        this(delegate, new InMemoryHedgeVenueIdempotencyStore());
    }

    @Autowired
    public IdempotentHedgeVenueAdapter(
            @Qualifier("rejectingHedgeVenueAdapter") HedgeVenueAdapter delegate,
            HedgeVenueIdempotencyStore store
    ) {
        this.delegate = delegate;
        this.store = store;
    }

    @Override
    public HedgeOrderResult submit(HedgeOrderRequest request) {
        String key = key(request);
        String fingerprint = fingerprint(request);
        Optional<HedgeVenueIdempotencyRecord> existing =
                store.find(key);
        if (existing.isPresent()) {
            return duplicateResult(existing.get(), fingerprint);
        }

        if (!store.claim(key, fingerprint)) {
            return store.find(key)
                    .map(record -> duplicateResult(record, fingerprint))
                    .orElseGet(() -> HedgeOrderResult.retryableRejected("HEDGE_VENUE_OUTCOME_UNCERTAIN"));
        }

        HedgeOrderResult result = delegate.submit(request);
        store.complete(key, fingerprint, result);
        return result;
    }

    private static HedgeOrderResult duplicateResult(
            HedgeVenueIdempotencyRecord record,
            String fingerprint
    ) {
        if (!record.fingerprint().equals(fingerprint)) {
            return HedgeOrderResult.rejected("HEDGE_VENUE_IDEMPOTENCY_CONFLICT");
        }

        if (!record.completed() || record.result() == null || record.result().retryable()) {
            return HedgeOrderResult.retryableRejected("HEDGE_VENUE_OUTCOME_UNCERTAIN");
        }

        return record.result();
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

}
