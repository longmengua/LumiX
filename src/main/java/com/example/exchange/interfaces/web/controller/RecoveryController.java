/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.SnapshotRecoverCommand;
import com.example.exchange.application.service.OutboxService;
import com.example.exchange.application.service.ReconciliationService;
import com.example.exchange.application.usecase.SnapshotRecoverUseCase;
import com.example.exchange.domain.model.dto.RecoveryResult;
import com.example.exchange.domain.model.dto.ValidationIssue;
import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 快照與恢復 API
 * - DEMO：提供恢復按鈕；實務上 Snapshot 產生會由 Scheduler 或事件觸發
 */
@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final SnapshotRecoverUseCase usecase;
    private final ReconciliationService reconciliationService;
    private final OutboxService outboxService;

    public RecoveryController(
            SnapshotRecoverUseCase usecase,
            ReconciliationService reconciliationService,
            OutboxService outboxService
    ) {
        this.usecase = usecase;
        this.reconciliationService = reconciliationService;
        this.outboxService = outboxService;
    }

    @PostMapping("/recover/{uid}")
    public ApiResponse<RecoveryResult> recover(
            @PathVariable long uid,
            @RequestParam(required = false) Long fromSeq
    ) {
        return ApiResponse.ok(usecase.handle(new SnapshotRecoverCommand(uid, fromSeq)));
    }

    @GetMapping("/validate/{uid}")
    public ApiResponse<List<ValidationIssue>> validate(@PathVariable long uid) {
        return ApiResponse.ok(reconciliationService.validateUid(uid));
    }

    @GetMapping("/reconcile/accounts")
    public ApiResponse<List<ValidationIssue>> validateAllAccounts() {
        return ApiResponse.ok(reconciliationService.validateAllAccounts());
    }

    @GetMapping("/outbox/dlq")
    public ApiResponse<List<DlqEvent>> latestDlq(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(outboxService.latestDlq(limit));
    }

    @PostMapping("/outbox/dead/{outboxId}/replay")
    public ApiResponse<OutboxEvent> replayDeadOutbox(@PathVariable UUID outboxId) {
        return ApiResponse.ok(outboxService.replayDead(outboxId));
    }

    @PostMapping("/outbox/dead/{outboxId}/compensate")
    public ApiResponse<OutboxEvent> compensateDeadOutbox(
            @PathVariable UUID outboxId,
            @RequestParam(defaultValue = "manual") String reason
    ) {
        return ApiResponse.ok(outboxService.markCompensated(outboxId, reason));
    }
}
