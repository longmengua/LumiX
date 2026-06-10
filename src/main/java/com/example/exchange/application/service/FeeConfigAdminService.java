/*
 * File purpose: Admin application service for changing market fee rates with audit history.
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.FeeConfigChangeRecord;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.repository.jpa.FeeConfigChangeRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeConfigAdminService {

    private static final BigDecimal MAX_FEE_RATE = new BigDecimal("0.100000000000000000");

    private final SymbolConfigRepository symbolConfigRepository;
    private final FeeConfigChangeRecordJpaRepository changeLogRepository;
    private final Clock clock = Clock.systemUTC();

    public FeeConfigChangeRecord updateFees(
            String symbol,
            BigDecimal makerFeeRate,
            BigDecimal takerFeeRate,
            String operatorId,
            String reason,
            String requestId,
            Instant effectiveAt
    ) {
        String normalizedSymbol = normalizeSymbol(symbol);
        BigDecimal normalizedMaker = validateFeeRate("makerFeeRate", makerFeeRate);
        BigDecimal normalizedTaker = validateFeeRate("takerFeeRate", takerFeeRate);
        String normalizedOperator = requireText("operatorId", operatorId);
        String normalizedReason = requireText("reason", reason);
        Instant changedAt = Instant.now(clock);
        Instant normalizedEffectiveAt = effectiveAt == null ? changedAt : effectiveAt;

        SymbolConfig config = symbolConfigRepository.findBySymbol(normalizedSymbol)
                .orElseThrow(() -> new IllegalArgumentException("unsupported symbol: " + normalizedSymbol));
        BigDecimal oldMaker = config.makerFeeRateOrDefault();
        BigDecimal oldTaker = config.takerFeeRateOrDefault();

        // The MVP applies the new runtime fee immediately; order snapshots protect existing resting orders.
        config.setMakerFeeRate(normalizedMaker);
        config.setTakerFeeRate(normalizedTaker);
        symbolConfigRepository.save(config);

        FeeConfigChangeRecord record = FeeConfigChangeRecord.create(
                normalizedSymbol,
                oldMaker,
                oldTaker,
                normalizedMaker,
                normalizedTaker,
                normalizedOperator,
                normalizedReason,
                blankToNull(requestId),
                normalizedEffectiveAt,
                changedAt
        );
        return changeLogRepository.save(record);
    }

    public List<FeeConfigChangeRecord> recentChanges(String symbol) {
        return changeLogRepository.findTop20BySymbolOrderByChangedAtDesc(normalizeSymbol(symbol));
    }

    private static String normalizeSymbol(String symbol) {
        return requireText("symbol", symbol).toUpperCase();
    }

    private static BigDecimal validateFeeRate(String field, BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.signum() < 0 || value.compareTo(MAX_FEE_RATE) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and 0.10");
        }
        return value.stripTrailingZeros();
    }

    private static String requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
