/*
 * 檔案用途：應用層排程任務，定期保存 account risk snapshots。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.AccountRiskSnapshotService;
import com.example.exchange.infra.config.RiskSnapshotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountRiskSnapshotScheduler {

    private final AccountRiskSnapshotService snapshotService;
    private final RiskSnapshotProperties properties;

    @Scheduled(cron = "${risk-snapshots.cron:0 0 0 * * *}", zone = "${risk-snapshots.zone:UTC}")
    public void persistDailySnapshots() {
        if (!properties.isEnabled()) return;
        snapshotService.persistKnownAccounts();
    }
}
