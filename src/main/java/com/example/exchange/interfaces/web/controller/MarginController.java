/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.TransferMarginCommand;
import com.example.exchange.application.service.AccountRiskSnapshotService;
import com.example.exchange.application.service.AccountRiskService;
import com.example.exchange.application.service.BonusCreditService;
import com.example.exchange.application.service.MarginService;
import com.example.exchange.application.service.TurnoverReconciliationService;
import com.example.exchange.application.service.TurnoverService;
import com.example.exchange.application.service.WalletLedgerReplayService;
import com.example.exchange.application.usecase.TransferMarginUseCase;
import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.dto.BonusCreditCampaignReport;
import com.example.exchange.domain.model.dto.BonusCreditCampaignExport;
import com.example.exchange.domain.model.dto.BonusCreditReport;
import com.example.exchange.domain.model.dto.TransferReconciliationProjection;
import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.dto.TurnoverExportReport;
import com.example.exchange.domain.model.dto.TurnoverReconciliationBatchReport;
import com.example.exchange.domain.model.dto.TurnoverReconciliationReport;
import com.example.exchange.domain.model.dto.TurnoverSummary;
import com.example.exchange.domain.model.dto.WalletLedgerReplayResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.interfaces.web.dto.AccountResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.BonusCreditClawbackRequest;
import com.example.exchange.interfaces.web.dto.DepositCallbackRequest;
import com.example.exchange.interfaces.web.dto.DepositRequest;
import com.example.exchange.interfaces.web.dto.PositionResponse;
import com.example.exchange.interfaces.web.dto.TransferRequest;
import com.example.exchange.interfaces.web.dto.TransferReviewClaimRequest;
import com.example.exchange.interfaces.web.dto.WithdrawalRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final BonusCreditService bonusCreditService;
    private final TurnoverService turnoverService;
    private final TurnoverReconciliationService turnoverReconciliationService;
    private final PositionRepository positionRepository;

    public MarginController(
            TransferMarginUseCase usecase,
            MarginService marginService,
            AccountRiskService accountRiskService,
            AccountRiskSnapshotService accountRiskSnapshotService,
            WalletLedgerReplayService walletLedgerReplayService,
            BonusCreditService bonusCreditService,
            TurnoverService turnoverService,
            TurnoverReconciliationService turnoverReconciliationService,
            PositionRepository positionRepository
    ) {
        this.usecase = usecase;
        this.marginService = marginService;
        this.accountRiskService = accountRiskService;
        this.accountRiskSnapshotService = accountRiskSnapshotService;
        this.walletLedgerReplayService = walletLedgerReplayService;
        this.bonusCreditService = bonusCreditService;
        this.turnoverService = turnoverService;
        this.turnoverReconciliationService = turnoverReconciliationService;
        this.positionRepository = positionRepository;
    }

    @PostMapping("/deposit")
    public ApiResponse<WalletTransfer> deposit(@Valid @RequestBody DepositRequest request) {
        return ApiResponse.ok(marginService.deposit(request.uid(), request.asset(), request.amount()));
    }

    @PostMapping("/deposit/callback")
    public ApiResponse<WalletTransfer> depositCallback(@Valid @RequestBody DepositCallbackRequest request) {
        return ApiResponse.ok(marginService.recordDepositCallback(
                request.uid(),
                request.asset(),
                request.amount(),
                request.externalRef()
        ));
    }

    @PostMapping("/withdraw")
    public ApiResponse<WalletTransfer> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ApiResponse.ok(marginService.withdraw(request.uid(), request.asset(), request.amount()));
    }

    @PostMapping("/transfer")
    public ApiResponse<String> transfer(@Valid @RequestBody TransferRequest r) {
        usecase.handle(new TransferMarginCommand(r.uid(), r.symbol(), r.toIsolated(), r.amount()));
        return ApiResponse.ok("ok");
    }

    @GetMapping("/account")
    public ApiResponse<AccountResponse> account(@RequestParam Long uid) {
        // Return a web DTO instead of the Account domain object so JSON fields stay stable for clients.
        return ApiResponse.ok(marginService.findAccount(uid).map(AccountResponse::from).orElse(null));
    }

    @GetMapping("/positions")
    public ApiResponse<List<PositionResponse>> positions(
            @RequestParam Long uid,
            @RequestParam(required = false) String symbol
    ) {
        String normalizedSymbol = symbol == null || symbol.isBlank() ? null : symbol.trim().toUpperCase();
        // The client needs filled orders to move from "委託" into a current-position view after matching.
        return ApiResponse.ok(positionRepository.findAllByUid(uid).stream()
                .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                .filter(position -> normalizedSymbol == null
                        || (position.getSymbol() != null && normalizedSymbol.equals(position.getSymbol().code())))
                .map(PositionResponse::from)
                .toList());
    }

    @GetMapping("/ledger")
    public ApiResponse<List<WalletLedgerEntry>> ledger(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findLedger(uid));
    }

    @GetMapping("/bonus-credit/report")
    public ApiResponse<BonusCreditReport> bonusCreditReport(
            @RequestParam Long uid,
            @RequestParam(required = false) String asset
    ) {
        return ApiResponse.ok(bonusCreditService.report(uid, asset));
    }

    @GetMapping("/bonus-credit/campaign-report")
    public ApiResponse<BonusCreditCampaignReport> bonusCreditCampaignReport(
            @RequestParam String campaignId,
            @RequestParam(required = false) String asset
    ) {
        return ApiResponse.ok(bonusCreditService.campaignReport(campaignId, asset));
    }

    @GetMapping("/bonus-credit/campaign-export")
    public ApiResponse<BonusCreditCampaignExport> bonusCreditCampaignExport(
            @RequestParam String campaignId,
            @RequestParam(required = false) String asset
    ) {
        return ApiResponse.ok(bonusCreditService.campaignExport(campaignId, asset));
    }

    @PostMapping("/bonus-credit/clawback")
    public ApiResponse<BigDecimal> clawbackBonusCredit(
            @Valid @RequestBody BonusCreditClawbackRequest request
    ) {
        return ApiResponse.ok(bonusCreditService.clawback(
                request.uid(),
                request.asset(),
                request.amount(),
                request.refId()
        ));
    }

    @GetMapping("/turnover/summary")
    public ApiResponse<TurnoverSummary> turnoverSummary(
            @RequestParam Long uid,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String marketMakerId,
            @RequestParam(required = false) String matchId
    ) {
        return ApiResponse.ok(turnoverService.summarize(uid, symbol, strategyId, marketMakerId, matchId));
    }

    @GetMapping("/turnover/records")
    public ApiResponse<List<TurnoverRecord>> turnoverRecords(
            @RequestParam Long uid,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String marketMakerId,
            @RequestParam(required = false) String matchId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(turnoverService.records(uid, symbol, strategyId, marketMakerId, matchId, limit));
    }

    @GetMapping("/turnover/export")
    public ApiResponse<TurnoverExportReport> turnoverExport(
            @RequestParam Long uid,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String marketMakerId,
            @RequestParam(required = false) String matchId,
            @RequestParam(defaultValue = "500") int limit
    ) {
        return ApiResponse.ok(turnoverService.export(uid, symbol, strategyId, marketMakerId, matchId, limit));
    }

    @GetMapping("/turnover/reconciliation")
    public ApiResponse<TurnoverReconciliationReport> turnoverReconciliation(
            @RequestParam Long uid,
            @RequestParam String matchId
    ) {
        return ApiResponse.ok(turnoverReconciliationService.reconcileMatch(uid, matchId));
    }

    @GetMapping("/turnover/reconciliation/recent")
    public ApiResponse<TurnoverReconciliationBatchReport> turnoverRecentReconciliation(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "1000") int limit
    ) {
        return ApiResponse.ok(turnoverReconciliationService.reconcileRecent(from, to, limit));
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
