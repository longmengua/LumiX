/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.TransferMarginCommand;
import com.example.exchange.application.service.AccountRiskSnapshotService;
import com.example.exchange.application.service.AccountRiskService;
import com.example.exchange.application.service.MarginService;
import com.example.exchange.application.service.WalletLedgerReplayService;
import com.example.exchange.application.usecase.TransferMarginUseCase;
import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.dto.TransferReconciliationProjection;
import com.example.exchange.domain.model.dto.WalletLedgerReplayResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.DepositCallbackRequest;
import com.example.exchange.interfaces.web.dto.DepositRequest;
import com.example.exchange.interfaces.web.dto.TransferRequest;
import com.example.exchange.interfaces.web.dto.TransferReviewClaimRequest;
import com.example.exchange.interfaces.web.dto.WithdrawalRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 劃轉相關 REST API */
@RestController
@RequestMapping("/api/margin")
public class MarginController {

    private final TransferMarginUseCase usecase;
    private final MarginService marginService;
    private final AccountRiskService accountRiskService;
    private final AccountRiskSnapshotService accountRiskSnapshotService;
    private final WalletLedgerReplayService walletLedgerReplayService;

    public MarginController(
            TransferMarginUseCase usecase,
            MarginService marginService,
            AccountRiskService accountRiskService,
            AccountRiskSnapshotService accountRiskSnapshotService,
            WalletLedgerReplayService walletLedgerReplayService
    ) {
        this.usecase = usecase;
        this.marginService = marginService;
        this.accountRiskService = accountRiskService;
        this.accountRiskSnapshotService = accountRiskSnapshotService;
        this.walletLedgerReplayService = walletLedgerReplayService;
    }

    @PostMapping("/deposit")
    public ApiResponse<WalletTransfer> deposit(@Valid @RequestBody DepositRequest request) {
        return ApiResponse.ok(marginService.deposit(request.uid(), request.amount()));
    }

    @PostMapping("/deposit/callback")
    public ApiResponse<WalletTransfer> depositCallback(@Valid @RequestBody DepositCallbackRequest request) {
        return ApiResponse.ok(marginService.recordDepositCallback(
                request.uid(),
                request.amount(),
                request.externalRef()
        ));
    }

    @PostMapping("/withdraw")
    public ApiResponse<WalletTransfer> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ApiResponse.ok(marginService.withdraw(request.uid(), request.amount()));
    }

    @PostMapping("/transfer")
    public ApiResponse<String> transfer(@Valid @RequestBody TransferRequest r) {
        usecase.handle(new TransferMarginCommand(r.uid(), r.symbol(), r.toIsolated(), r.amount()));
        return ApiResponse.ok("ok");
    }

    @GetMapping("/account")
    public ApiResponse<Account> account(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findAccount(uid).orElse(null));
    }

    @GetMapping("/ledger")
    public ApiResponse<List<WalletLedgerEntry>> ledger(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findLedger(uid));
    }

    @GetMapping("/ledger/replay")
    public ApiResponse<WalletLedgerReplayResult> replayLedger(
            @RequestParam Long uid,
            @RequestParam(required = false) String asset
    ) {
        return ApiResponse.ok(walletLedgerReplayService.replay(uid, asset));
    }

    @GetMapping("/ledger/replay/compare")
    public ApiResponse<WalletLedgerReplayResult> compareLedgerReplay(
            @RequestParam Long uid,
            @RequestParam(required = false) String asset
    ) {
        return ApiResponse.ok(walletLedgerReplayService.replayAndCompareAccount(uid, asset));
    }

    @GetMapping("/transfers")
    public ApiResponse<List<WalletTransfer>> transfers(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findTransfers(uid));
    }

    @PostMapping("/transfers/manual-review/claim")
    public ApiResponse<WalletTransfer> claimTransferReview(
            @Valid @RequestBody TransferReviewClaimRequest request
    ) {
        return ApiResponse.ok(marginService.claimManualReview(
                request.transferId(),
                request.owner()
        ));
    }

    @GetMapping("/transfers/reconciliation")
    public ApiResponse<List<TransferReconciliationProjection>> transferReconciliation(
            @RequestParam Long uid
    ) {
        return ApiResponse.ok(marginService.transferReconciliation(uid));
    }

    @GetMapping("/risk")
    public ApiResponse<AccountRiskSnapshot> risk(@RequestParam Long uid) {
        return ApiResponse.ok(accountRiskService.snapshot(uid));
    }

    @PostMapping("/risk/snapshot")
    public ApiResponse<AccountRiskSnapshot> persistRiskSnapshot(@RequestParam Long uid) {
        return ApiResponse.ok(accountRiskSnapshotService.persist(uid));
    }

    @PostMapping("/risk/snapshots")
    public ApiResponse<List<AccountRiskSnapshot>> persistKnownRiskSnapshots() {
        return ApiResponse.ok(accountRiskSnapshotService.persistKnownAccounts());
    }

    @GetMapping("/risk/snapshot/latest")
    public ApiResponse<AccountRiskSnapshot> latestRiskSnapshot(@RequestParam Long uid) {
        return ApiResponse.ok(accountRiskSnapshotService.latest(uid).orElse(null));
    }

    @GetMapping("/risk/snapshots")
    public ApiResponse<List<AccountRiskSnapshot>> riskSnapshotHistory(
            @RequestParam Long uid,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return ApiResponse.ok(accountRiskSnapshotService.history(uid, limit));
    }
}
