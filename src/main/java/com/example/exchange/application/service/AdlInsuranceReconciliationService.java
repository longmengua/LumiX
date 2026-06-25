/*
 * 檔案用途：對帳 ADL queue shortfall、liquidated position coverage 與 insurance fund 狀態。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlInsuranceReconciliationIssue;
import com.example.exchange.domain.model.dto.AdlInsuranceReconciliationReport;
import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdlInsuranceReconciliationService {

    private final InsuranceFundService insuranceFundService;
    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public AdlInsuranceReconciliationReport reconcile(String asset) {
        String normalizedAsset = normalizeAsset(asset);
        List<AdlQueueEntry> queue = insuranceFundService.adlQueue();
        BigDecimal openAmount = queue.stream()
                .map(AdlQueueEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<AdlInsuranceReconciliationIssue> issues = new ArrayList<>();
        for (AdlQueueEntry entry : queue) {
            reconcileEntry(entry, issues);
        }
        return new AdlInsuranceReconciliationReport(
                normalizedAsset,
                insuranceFundService.balance(normalizedAsset),
                queue.size(),
                openAmount,
                issues.size(),
                Instant.now(),
                issues
        );
    }

    private void reconcileEntry(AdlQueueEntry entry, List<AdlInsuranceReconciliationIssue> issues) {
        Position position = positionRepository.findAllByUid(entry.uid()).stream()
                .filter(candidate -> candidate.getSymbol() != null && entry.symbol().equals(candidate.getSymbol().code()))
                .findFirst()
                .orElse(null);
        if (position == null) {
            issues.add(issue(entry, null, "MISSING_LIQUIDATED_POSITION"));
            return;
        }
        BigDecimal adlCovered = safe(position.getAdlCovered());
        if (adlCovered.signum() <= 0) {
            issues.add(issue(entry, position, "MISSING_POSITION_ADL_COVERAGE"));
        }
        if (safe(entry.amount()).compareTo(adlCovered) > 0) {
            issues.add(issue(entry, position, "QUEUE_EXCEEDS_POSITION_ADL_COVERAGE"));
        }
    }

    private static AdlInsuranceReconciliationIssue issue(AdlQueueEntry entry, Position position, String reason) {
        return new AdlInsuranceReconciliationIssue(
                entry.liquidationId(),
                entry.uid(),
                entry.symbol(),
                reason,
                entry.amount(),
                position == null ? BigDecimal.ZERO : position.getAdlCovered(),
                position == null ? BigDecimal.ZERO : position.getInsuranceFundCovered(),
                entry.owner()
        );
    }

    private static String normalizeAsset(String asset) {
        return asset == null || asset.isBlank() ? "USDT" : asset.trim().toUpperCase();
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
