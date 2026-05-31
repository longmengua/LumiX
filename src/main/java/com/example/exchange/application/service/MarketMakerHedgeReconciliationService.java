/*
 * 檔案用途：應用服務，對帳做市商 hedge decision audit 與 venue fill audit trail。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;
import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.dto.HedgeReconciliationIssue;
import com.example.exchange.domain.model.dto.HedgeReconciliationReport;
import com.example.exchange.domain.repository.HedgeDecisionAuditStore;
import com.example.exchange.domain.repository.HedgeFillStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgeReconciliationService {

    private static final int MAX_QUERY_LIMIT = 500;

    private final HedgeDecisionAuditStore decisionAuditStore;
    private final HedgeFillStore hedgeFillStore;

    @Transactional(readOnly = true)
    public HedgeReconciliationReport reconcileMarketMaker(String marketMakerId, int limit) {
        validateQueryLimit(limit);
        List<HedgeDecisionAuditRecord> decisions = decisionAuditStore.findByMarketMakerId(marketMakerId, limit)
                .stream()
                .filter(HedgeDecisionAuditRecord::accepted)
                .toList();
        List<HedgeReconciliationIssue> issues = new ArrayList<>();
        for (HedgeDecisionAuditRecord decision : decisions) {
            issues.addAll(reconcileDecision(decision));
        }
        return new HedgeReconciliationReport(
                marketMakerId,
                decisions.size(),
                issues.size(),
                Instant.now(),
                issues
        );
    }

    private List<HedgeReconciliationIssue> reconcileDecision(HedgeDecisionAuditRecord decision) {
        List<HedgeReconciliationIssue> issues = new ArrayList<>();
        if (isBlank(decision.internalTradeRefId())) {
            issues.add(issue(decision, null, "MISSING_INTERNAL_TRADE_REF", BigDecimal.ZERO));
        }
        if (decision.venueOrderId() == null || decision.venueOrderId().isBlank()) {
            issues.add(issue(decision, null, "MISSING_VENUE_ORDER", BigDecimal.ZERO));
            return issues;
        }
        List<HedgeFillRecord> fills = hedgeFillStore.findByVenueOrderId(decision.venueOrderId());
        if (fills.isEmpty()) {
            issues.add(issue(decision, null, "MISSING_FILL", BigDecimal.ZERO));
            return issues;
        }
        for (HedgeFillRecord fill : fills) {
            if (isBlank(fill.ledgerRefId())) {
                issues.add(issue(decision, fill, "MISSING_LEDGER_REF", BigDecimal.ZERO));
            }
            if (!refsMatch(decision, fill)) {
                issues.add(issue(decision, fill, "TRADE_LEDGER_REF_MISMATCH", BigDecimal.ZERO));
            }
        }
        BigDecimal filledNotional = fills.stream()
                .map(HedgeFillRecord::notional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int compare = filledNotional.compareTo(decision.orderNotional());
        if (compare < 0) {
            issues.add(issue(decision, null, "UNDERFILLED_NOTIONAL", filledNotional));
            return issues;
        }
        if (compare > 0) {
            issues.add(issue(decision, null, "OVERFILLED_NOTIONAL", filledNotional));
            return issues;
        }
        return issues;
    }

    private static HedgeReconciliationIssue issue(
            HedgeDecisionAuditRecord decision,
            HedgeFillRecord fill,
            String reason,
            BigDecimal filledNotional
    ) {
        return new HedgeReconciliationIssue(
                decision.marketMakerId(),
                decision.symbol(),
                decision.refId(),
                decision.venueOrderId(),
                decision.internalTradeRefId(),
                fill == null ? null : fill.ledgerRefId(),
                reason,
                decision.orderNotional(),
                filledNotional
        );
    }

    private static boolean refsMatch(HedgeDecisionAuditRecord decision, HedgeFillRecord fill) {
        if (isBlank(decision.internalTradeRefId()) || isBlank(fill.refId())) {
            return true;
        }
        return decision.internalTradeRefId().equals(fill.refId());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void validateQueryLimit(int limit) {
        if (limit <= 0 || limit > MAX_QUERY_LIMIT) {
            throw new IllegalArgumentException("hedge reconciliation query limit must be between 1 and " + MAX_QUERY_LIMIT);
        }
    }
}
