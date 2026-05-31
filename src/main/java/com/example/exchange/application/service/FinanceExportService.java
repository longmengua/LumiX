/*
 * 檔案用途：每日財務分類匯出服務，批次產生 fee/funding/liquidation/bonus/transfer reports。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.FinanceCategoryExportBatch;
import com.example.exchange.domain.model.dto.FinanceDailyReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceExportService {

    public static final List<String> DAILY_CATEGORIES = List.of("fee", "funding", "liquidation", "bonus", "transfer");

    private final FinanceReportService financeReportService;

    public FinanceCategoryExportBatch exportDailyCategories(LocalDate reportDate) {
        LocalDate date = reportDate == null ? LocalDate.now(ZoneOffset.UTC).minusDays(1) : reportDate;
        List<FinanceDailyReport> reports = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        for (String category : DAILY_CATEGORIES) {
            FinanceDailyReport report = financeReportService.categoryReport(date, category);
            reports.add(report);
            if (!report.balanced()) {
                blockers.add("UNBALANCED_CATEGORY:" + report.reportDate() + ":" + category);
            }
        }
        return new FinanceCategoryExportBatch(
                date,
                "finance-category-" + date,
                blockers.isEmpty(),
                Instant.now(),
                reports,
                blockers
        );
    }

}
