/*
 * 檔案用途：archive exporter skeleton，產生歷史訂單、成交與 ledger archive plan。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.ArchiveExportPlan;
import com.example.exchange.domain.model.dto.ArchiveExporterRunReport;
import com.example.exchange.domain.model.dto.LedgerArchiveDeleteGuardReport;
import com.example.exchange.domain.model.dto.LedgerArchiveManifest;
import com.example.exchange.infra.config.ArchiveExporterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveExporterService {

    private final ArchiveExporterProperties properties;
    private final LedgerArchiveManifestService ledgerArchiveManifestService;

    public ArchiveExporterRunReport run(LocalDate archiveDate) {
        LocalDate date =
                archiveDate == null
                        ? LocalDate.now(ZoneOffset.UTC).minusDays(Math.max(1L, properties.getDaysBack()))
                        : archiveDate;
        if (!properties.isEnabled()) {
            return new ArchiveExporterRunReport(false, date, Instant.now(), List.of());
        }

        List<ArchiveExportPlan> plans =
                new ArrayList<>();
        if (properties.isOrdersEnabled()) {
            plans.add(orderPlan(date));
        }
        if (properties.isTradesEnabled()) {
            plans.add(tradePlan(date));
        }
        if (properties.isLedgerEnabled()) {
            plans.add(ledgerPlan(date));
        }

        return new ArchiveExporterRunReport(true, date, Instant.now(), plans);
    }

    private ArchiveExportPlan orderPlan(LocalDate date) {
        return new ArchiveExportPlan(
                "historical_orders",
                "order_lifecycle_events,order_lifecycle_projection",
                "event_date=" + date,
                "orders-" + date,
                false,
                List.of(
                        "terminal state verified",
                        "lifecycle event count exported",
                        "replay sample passed",
                        "Redis order secondary indexes cleaned after archive verification"
                )
        );
    }

    private ArchiveExportPlan tradePlan(LocalDate date) {
        return new ArchiveExportPlan(
                "trades",
                "matching_event_logs,market_data_trade_tape,kafka.trade.executed",
                "trade_date=" + date,
                "trades-" + date,
                false,
                List.of(
                        "matching event offset checkpoint exported",
                        "finance consumers caught up",
                        "market-data consumers caught up",
                        "archive manifest checksum recorded"
                )
        );
    }

    private ArchiveExportPlan ledgerPlan(LocalDate date) {
        LedgerArchiveManifest manifest =
                ledgerArchiveManifestService.generate(date);
        LedgerArchiveDeleteGuardReport deleteGuard =
                ledgerArchiveManifestService.deleteGuard(date);

        return new ArchiveExportPlan(
                "wallet_ledger",
                "wallet_ledger_entries,wallet_ledger_postings",
                "created_date=" + date,
                manifest.archiveBatchId(),
                deleteGuard.approved(),
                deleteGuard.blockers().isEmpty()
                        ? List.of("ledger manifest generated", "restore smoke passed", "replay validation passed")
                        : deleteGuard.blockers()
        );
    }
}
