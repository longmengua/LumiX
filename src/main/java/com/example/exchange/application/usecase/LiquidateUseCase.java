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
        return liquidationService.liquidate(cmd.uid(), cmd.symbol(), cmd.markPrice());
    }
}
