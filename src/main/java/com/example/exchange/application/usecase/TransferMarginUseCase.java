/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.TransferMarginCommand;
import com.example.exchange.application.service.MarginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 劃轉用例 */
@Component
@RequiredArgsConstructor
public class TransferMarginUseCase {

    private final MarginService svc;

    public void handle(TransferMarginCommand cmd) {
        svc.transferIsolated(cmd.uid(), cmd.symbol(), cmd.toIsolated(), cmd.amount());
    }
}
