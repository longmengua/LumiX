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

    private final HedgeDecisionAuditStore decisionAuditStore;
    private final HedgeFillStore hedgeFillStore;

    @Transactional(readOnly = true)
    public HedgeReconciliationReport reconcileMarketMaker(String marketMakerId, int limit) {
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
        if (decision.venueOrderId() == null || decision.venueOrderId().isBlank()) {
            return List.of(issue(decision, "MISSING_VENUE_ORDER", BigDecimal.ZERO));
        }
        List<HedgeFillRecord> fills = hedgeFillStore.findByVenueOrderId(decision.venueOrderId());
        if (fills.isEmpty()) {
            return List.of(issue(decision, "MISSING_FILL", BigDecimal.ZERO));
        }
        BigDecimal filledNotional = fills.stream()
                .map(HedgeFillRecord::notional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int compare = filledNotional.compareTo(decision.orderNotional());
        if (compare < 0) {
            return List.of(issue(decision, "UNDERFILLED_NOTIONAL", filledNotional));
        }
        if (compare > 0) {
            return List.of(issue(decision, "OVERFILLED_NOTIONAL", filledNotional));
        }
        return List.of();
    }

    private static HedgeReconciliationIssue issue(
            HedgeDecisionAuditRecord decision,
            String reason,
            BigDecimal filledNotional
    ) {
        return new HedgeReconciliationIssue(
                decision.marketMakerId(),
                decision.symbol(),
                decision.refId(),
                decision.venueOrderId(),
                reason,
                decision.orderNotional(),
                filledNotional
        );
    }
}
