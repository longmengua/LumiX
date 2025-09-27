package com.example.java21_OLAP.application.usecase;

import com.example.java21_OLAP.application.command.TransferMarginCommand;
import com.example.java21_OLAP.application.service.MarginService;
import org.springframework.stereotype.Component;

/** 劃轉用例 */
@Component
public class TransferMarginUseCase {

    private final MarginService svc;

    public TransferMarginUseCase(MarginService svc) {
        this.svc = svc;
    }

    public void handle(TransferMarginCommand cmd) {
        svc.transferIsolated(cmd.uid(), cmd.symbol(), cmd.toIsolated(), cmd.amount());
    }
}
