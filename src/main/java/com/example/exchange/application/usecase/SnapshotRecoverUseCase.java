/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.SnapshotRecoverCommand;
import com.example.exchange.application.service.RecoveryService;
import com.example.exchange.domain.model.dto.RecoveryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 快照恢復用例 */
@Component
@RequiredArgsConstructor
public class SnapshotRecoverUseCase {

    private final RecoveryService svc;

    public RecoveryResult handle(SnapshotRecoverCommand cmd) {
        return svc.recover(cmd.uid(), cmd.fromSeq());
    }
}
