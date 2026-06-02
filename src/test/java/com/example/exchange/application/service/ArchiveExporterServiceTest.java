/*
 * 檔案用途：測試 archive exporter skeleton 的資料族 plan 與預設關閉行為。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.ArchiveExporterRunReport;
import com.example.exchange.domain.model.dto.LedgerArchiveDeleteGuardReport;
import com.example.exchange.domain.model.dto.LedgerArchiveManifest;
import com.example.exchange.infra.config.ArchiveExporterProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArchiveExporterServiceTest {

    @Test
    @DisplayName("disabled archive exporter 不產生 plan 且不觸發 ledger guard")
    void disabledExporterReturnsNoPlans() {
        ArchiveExporterProperties properties =
                new ArchiveExporterProperties();
        LedgerArchiveManifestService ledgerArchiveManifestService =
                mock(LedgerArchiveManifestService.class);
        ArchiveExporterService service =
                new ArchiveExporterService(properties, ledgerArchiveManifestService);

        ArchiveExporterRunReport report =
                service.run(LocalDate.parse("2026-06-02"));

        assertThat(report.enabled())
                .isFalse();
        assertThat(report.plans())
                .isEmpty();
        verifyNoInteractions(ledgerArchiveManifestService);
    }

    @Test
    @DisplayName("enabled archive exporter 產生 orders/trades/ledger 三類 skeleton plan")
    void enabledExporterBuildsOrdersTradesAndLedgerPlans() {
        ArchiveExporterProperties properties =
                new ArchiveExporterProperties();
        properties.setEnabled(true);
        LedgerArchiveManifestService ledgerArchiveManifestService =
                mock(LedgerArchiveManifestService.class);
        LocalDate archiveDate =
                LocalDate.parse("2026-06-02");
        when(ledgerArchiveManifestService.generate(archiveDate))
                .thenReturn(ledgerManifest(archiveDate));
        when(ledgerArchiveManifestService.deleteGuard(archiveDate))
                .thenReturn(approvedDeleteGuard(archiveDate));
        ArchiveExporterService service =
                new ArchiveExporterService(properties, ledgerArchiveManifestService);

        ArchiveExporterRunReport report =
                service.run(archiveDate);

        assertThat(report.enabled())
                .isTrue();
        assertThat(report.plans())
                .extracting("dataFamily")
                .containsExactly("historical_orders", "trades", "wallet_ledger");
        assertThat(report.plans().get(0).preconditions())
                .contains("terminal state verified");
        assertThat(report.plans().get(2).manifestRef())
                .isEqualTo("ledger-2026-06-02-test");
        assertThat(report.plans().get(2).deleteEligible())
                .isTrue();
        verify(ledgerArchiveManifestService)
                .generate(archiveDate);
        verify(ledgerArchiveManifestService)
                .deleteGuard(archiveDate);
    }

    private static LedgerArchiveManifest ledgerManifest(LocalDate archiveDate) {
        return new LedgerArchiveManifest(
                "ledger-" + archiveDate + "-test",
                "wallet_ledger",
                1,
                archiveDate,
                archiveDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                archiveDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                1,
                2,
                "checksum",
                true,
                null,
                Instant.now()
        );
    }

    private static LedgerArchiveDeleteGuardReport approvedDeleteGuard(LocalDate archiveDate) {
        return new LedgerArchiveDeleteGuardReport(
                archiveDate,
                true,
                Instant.now(),
                null,
                ledgerManifest(archiveDate),
                null,
                null,
                List.of()
        );
    }
}
