package com.example.exchange.application.usecase;

import com.example.exchange.application.command.SnapshotRecoverCommand;
import com.example.exchange.application.service.RecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 快照恢復用例 */
@Component
@RequiredArgsConstructor
public class SnapshotRecoverUseCase {

    private final RecoveryService svc;

    public void handle(SnapshotRecoverCommand cmd) {
        svc.recover(cmd.uid());
    }
}
