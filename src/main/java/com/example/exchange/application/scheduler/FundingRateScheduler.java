package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.FundingRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 資金費結算。
 * - 實務：每 8 小時結算一次（00:00 / 08:00 / 16:00 UTC）
 * - 需根據標記價/指數價與資金率計算支付/收取
 */
@Component
@RequiredArgsConstructor
public class FundingRateScheduler {

    private final FundingRateService fundingRateService;

    /** 每 8 小時觸發一次（示範 cron，請按實際時區/對齊時間調整） */
//    @Scheduled(cron = "0 0 */8 * * *")
    public void settle() {
        fundingRateService.settleConfiguredSymbols();
    }
}
