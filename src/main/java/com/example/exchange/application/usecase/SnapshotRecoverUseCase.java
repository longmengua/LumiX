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
