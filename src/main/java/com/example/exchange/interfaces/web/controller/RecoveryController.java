package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.SnapshotRecoverCommand;
import com.example.exchange.application.usecase.SnapshotRecoverUseCase;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 快照與恢復 API
 * - DEMO：提供恢復按鈕；實務上 Snapshot 產生會由 Scheduler 或事件觸發
 */
@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final SnapshotRecoverUseCase usecase;

    public RecoveryController(SnapshotRecoverUseCase usecase) {
        this.usecase = usecase;
    }

    @PostMapping("/recover/{uid}")
    public ApiResponse<String> recover(@PathVariable long uid) {
        usecase.handle(new SnapshotRecoverCommand(uid));
        return ApiResponse.ok("recovered");
    }
}
