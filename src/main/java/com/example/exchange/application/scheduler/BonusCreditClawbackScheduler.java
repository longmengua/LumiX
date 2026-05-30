/*
 * 檔案用途：排程入口，依設定自動追回指定 campaign 的 active 體驗金。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.BonusCreditService;
import com.example.exchange.infra.config.BonusCreditProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BonusCreditClawbackScheduler {

    private final BonusCreditService bonusCreditService;
    private final BonusCreditProperties properties;

    @Scheduled(fixedDelayString = "${bonus-credit.clawback-policy.fixed-delay-ms:300000}")
    public void clawbackCampaignBonusCredits() {
        BonusCreditProperties.ClawbackPolicy policy = properties.getClawbackPolicy();
        if (policy == null || !policy.isEnabled()) {
            return;
        }
        String refId = (policy.getRefPrefix() == null || policy.getRefPrefix().isBlank()
                ? "bonus-auto-clawback"
                : policy.getRefPrefix().trim()) + ":" + Instant.now().toEpochMilli();
        // Service 層會限制 campaign / asset / max amount，並只處理 ACTIVE 且未到期 grants。
        bonusCreditService.clawbackCampaign(
                policy.getCampaignId(),
                policy.getAsset(),
                policy.getMaxAmountPerRun(),
                refId
        );
    }
}
