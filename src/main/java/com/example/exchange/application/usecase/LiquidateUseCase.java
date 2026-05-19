/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.LiquidateCommand;
import com.example.exchange.application.service.LiquidationService;
import com.example.exchange.domain.model.dto.LiquidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 強平用例。
 */
@Component
@RequiredArgsConstructor
public class LiquidateUseCase {

    private final LiquidationService liquidationService;

    public LiquidationResult handle(LiquidateCommand cmd) {
        if (cmd.markPrice() == null) {
            return liquidationService.liquidate(cmd.uid(), cmd.symbol());
        }
        return liquidationService.liquidate(cmd.uid(), cmd.symbol(), cmd.markPrice());
    }
}
