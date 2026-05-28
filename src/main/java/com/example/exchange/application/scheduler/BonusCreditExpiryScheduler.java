/*
 * 檔案用途：排程入口，定期掃描已到期體驗金批次並寫入 expire ledger。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.BonusCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BonusCreditExpiryScheduler {

    private final BonusCreditService bonusCreditService;

    @Value("${bonus-credit.expiry-enabled:false}")
    private boolean enabled;

    @Value("${bonus-credit.expiry-batch-size:500}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${bonus-credit.expiry-fixed-delay-ms:300000}")
    public void expireDueBonusCredits() {
        if (!enabled) {
            return;
        }
        // Service 層保證每個 grant 只會從 ACTIVE 轉出一次，scheduler 重試不應重複扣 ledger。
        bonusCreditService.expireDue(batchSize);
    }
}
