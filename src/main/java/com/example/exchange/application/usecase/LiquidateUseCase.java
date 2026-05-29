/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.LiquidateCommand;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.application.service.LiquidationService;
import com.example.exchange.domain.model.dto.LiquidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 強平用例。
 */
@Component
@RequiredArgsConstructor
public class LiquidateUseCase {

    private final LiquidationService liquidationService;
    private CommandTransactionBoundary commandTransactionBoundary;

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
    }

    public LiquidationResult handle(LiquidateCommand cmd) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute("liquidate-position", () -> handleInsideTransaction(cmd));
        }
        return handleInsideTransaction(cmd);
    }

    private LiquidationResult handleInsideTransaction(LiquidateCommand cmd) {
        if (cmd.markPrice() == null) {
            return liquidationService.liquidate(cmd.uid(), cmd.symbol());
        }
        return liquidationService.liquidate(cmd.uid(), cmd.symbol(), cmd.markPrice());
    }
}
