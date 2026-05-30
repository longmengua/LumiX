/*
 * 檔案用途：應用服務，提供 hedge venue effectful submit idempotency 的營運檢視。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyIssue;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyRecord;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyReport;
import com.example.exchange.domain.repository.HedgeVenueIdempotencyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgeVenueIdempotencyService {

    private static final int MAX_QUERY_LIMIT = 500;

    private final HedgeVenueIdempotencyStore store;

    @Transactional(readOnly = true)
    public HedgeVenueIdempotencyReport unresolved(int limit) {
        validateQueryLimit(limit);
        var issues = store.findUnresolved(limit).stream()
                .map(this::toIssue)
                .toList();
        return new HedgeVenueIdempotencyReport(
                issues.size(),
                Instant.now(),
                issues
        );
    }

    private HedgeVenueIdempotencyIssue toIssue(HedgeVenueIdempotencyRecord record) {
        HedgeOrderResult result = record.result();
        boolean retryable = result != null && result.retryable();
        return new HedgeVenueIdempotencyIssue(
                record.refId(),
                reason(record, result),
                record.completed(),
                retryable,
                result == null ? null : result.venueOrderId(),
                result == null ? null : result.submittedAt()
        );
    }

    private static String reason(HedgeVenueIdempotencyRecord record, HedgeOrderResult result) {
        if (!record.completed()) {
            return "HEDGE_VENUE_SUBMIT_PENDING";
        }
        if (result == null || result.reason() == null || result.reason().isBlank()) {
            return "HEDGE_VENUE_OUTCOME_UNCERTAIN";
        }
        return result.reason();
    }

    private static void validateQueryLimit(int limit) {
        if (limit <= 0 || limit > MAX_QUERY_LIMIT) {
            throw new IllegalArgumentException("hedge venue idempotency query limit must be between 1 and " + MAX_QUERY_LIMIT);
        }
    }
}
