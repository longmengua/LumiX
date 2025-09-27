package com.example.exchange.application.usecase;

import com.example.exchange.application.command.LiquidateCommand;
import org.springframework.stereotype.Component;

/**
 * 強平用例（骨架）
 * - 真實會連動風控、撮合、保險基金結算
 */
@Component
public class LiquidateUseCase {

    public void handle(LiquidateCommand cmd) {
        // TODO: 1) 計算維持保證金 & 破產價
        //       2) 產生清算訂單/成交事件
        //       3) 減倉/平倉 & 發布 PositionLiquidated 事件
    }
}
