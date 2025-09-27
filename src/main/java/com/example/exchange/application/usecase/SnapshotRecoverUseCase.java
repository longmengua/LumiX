package com.example.exchange.application.usecase;

import com.example.exchange.application.command.SnapshotRecoverCommand;
import com.example.exchange.application.service.RecoveryService;
import org.springframework.stereotype.Component;

/** 快照恢復用例 */
@Component
public class SnapshotRecoverUseCase {

    private final RecoveryService svc;

    public SnapshotRecoverUseCase(RecoveryService svc) {
        this.svc = svc;
    }

    public void handle(SnapshotRecoverCommand cmd) {
        svc.recover(cmd.uid());
    }
}
