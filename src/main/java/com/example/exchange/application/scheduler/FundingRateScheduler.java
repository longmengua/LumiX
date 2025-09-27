package com.example.exchange.application.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 資金費結算（骨架）
 * - 實務：每 8 小時結算一次（00:00 / 08:00 / 16:00 UTC）
 * - 需根據標記價/指數價與資金率計算支付/收取
 */
@Component
public class FundingRateScheduler {

    /** 每 8 小時觸發一次（示範 cron，請按實際時區/對齊時間調整） */
    @Scheduled(cron = "0 0 */8 * * *")
    public void settle() {
        // TODO:
        // 1) 計算每個 symbol 當前資金費率
        // 2) 對持倉帳務入帳（加減餘額 / 保證金）
        // 3) 記錄審計/發事件
    }
}
