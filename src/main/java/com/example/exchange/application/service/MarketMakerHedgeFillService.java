/*
 * 檔案用途：應用服務，保存與查詢做市商 hedge fills。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.dto.HedgeVenueFillMessage;
import com.example.exchange.domain.repository.HedgeFillStore;
import com.example.exchange.domain.service.HedgeVenueFillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgeFillService {

    private static final int MAX_QUERY_LIMIT = 500;

    private final HedgeFillStore hedgeFillStore;
    private final HedgeVenueFillMapper fillMapper = new HedgeVenueFillMapper();

    @Transactional
    public HedgeFillRecord recordFill(HedgeFillRecord fill) {
        validate(fill);
        var existing = hedgeFillStore.findByVenueOrderIdAndVenueFillId(
                fill.venueOrderId(),
                fill.venueFillId()
        );
        if (existing.isPresent()) {
            return existing.get();
        }
        hedgeFillStore.append(fill);
        return hedgeFillStore.findByVenueOrderIdAndVenueFillId(
                fill.venueOrderId(),
                fill.venueFillId()
        ).orElse(fill);
    }

    @Transactional
    public HedgeFillRecord recordVenueFill(HedgeVenueFillMessage message) {
        return recordFill(fillMapper.toRecord(message));
    }

    @Transactional(readOnly = true)
    public List<HedgeFillRecord> fillsByMarketMaker(String marketMakerId, int limit) {
        validateQueryLimit(limit);
        return hedgeFillStore.findByMarketMakerId(marketMakerId, limit);
    }

    @Transactional(readOnly = true)
    public List<HedgeFillRecord> fillsByVenueOrder(String venueOrderId) {
        return hedgeFillStore.findByVenueOrderId(venueOrderId);
    }

    @Transactional(readOnly = true)
    public List<HedgeFillRecord> fillsByRefId(String refId) {
        return hedgeFillStore.findByRefId(refId);
    }

    private static void validate(HedgeFillRecord fill) {
        if (fill == null) {
            throw new IllegalArgumentException("hedge fill cannot be null");
        }
        if (fill.marketMakerId() == null || fill.marketMakerId().isBlank()) {
            throw new IllegalArgumentException("market maker id is required");
        }
        if (fill.symbol() == null || fill.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (fill.venueOrderId() == null || fill.venueOrderId().isBlank()) {
            throw new IllegalArgumentException("venue order id is required");
        }
        if (fill.venueFillId() == null || fill.venueFillId().isBlank()) {
            throw new IllegalArgumentException("venue fill id is required");
        }
        if (fill.side() == null) {
            throw new IllegalArgumentException("side is required");
        }
        if (fill.quantity().signum() <= 0 || fill.price().signum() <= 0) {
            throw new IllegalArgumentException("fill quantity and price must be positive");
        }
    }

    private static void validateQueryLimit(int limit) {
        if (limit <= 0 || limit > MAX_QUERY_LIMIT) {
            throw new IllegalArgumentException("hedge fill query limit must be between 1 and " + MAX_QUERY_LIMIT);
        }
    }
}
