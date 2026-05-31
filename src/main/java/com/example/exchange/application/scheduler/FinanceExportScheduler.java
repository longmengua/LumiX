/*
 * 檔案用途：每日財務分類匯出排程入口，預設關閉，啟用後產生前一 UTC 日分類報表。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.FinanceExportService;
import com.example.exchange.domain.model.dto.FinanceCategoryExportBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceExportScheduler {

    private final FinanceExportService financeExportService;

    @Value("${finance.export.enabled:false}")
    private boolean enabled;

    @Scheduled(cron = "${finance.export.cron:0 30 1 * * *}", zone = "${finance.export.zone:UTC}")
    public void exportPreviousUtcDay() {
        if (!enabled) {
            return;
        }
        LocalDate reportDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        FinanceCategoryExportBatch batch = financeExportService.exportDailyCategories(reportDate);
        log.info("FINANCE_EXPORT reportDate={} batchId={} balanced={} reports={} blockers={}",
                batch.reportDate(),
                batch.exportBatchId(),
                batch.balanced(),
                batch.reports().size(),
                batch.blockers());
    }
}
