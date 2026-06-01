/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.SnapshotRecoverCommand;
import com.example.exchange.application.service.FinanceReportService;
import com.example.exchange.application.service.AccountPositionConsistencyService;
import com.example.exchange.application.service.LedgerArchiveEligibilityService;
import com.example.exchange.application.service.LedgerArchiveManifestService;
import com.example.exchange.application.service.OutboxService;
import com.example.exchange.application.service.OutboxDomainStateConsistencyService;
import com.example.exchange.application.service.ReconciliationIssueWorkflowService;
import com.example.exchange.application.service.ReconciliationReportService;
import com.example.exchange.application.service.ReconciliationService;
import com.example.exchange.application.service.MatchingWorkerLifecycleService;
import com.example.exchange.application.service.TrialBalanceService;
import com.example.exchange.application.service.WalletLedgerReplayService;
import com.example.exchange.domain.model.dto.LedgerReplayComparisonReport;
import com.example.exchange.domain.model.dto.AccountPositionConsistencyReport;
import com.example.exchange.domain.model.dto.LedgerArchiveDeleteGuardReport;
import com.example.exchange.domain.model.dto.LedgerArchiveEligibilityReport;
import com.example.exchange.domain.model.dto.LedgerArchiveManifest;
import com.example.exchange.domain.model.dto.LedgerArchiveReplayValidationReport;
import com.example.exchange.domain.model.dto.LedgerArchiveRestoreSmokeReport;
import com.example.exchange.domain.model.dto.LedgerTamperEvidenceReport;
import com.example.exchange.application.usecase.SnapshotRecoverUseCase;
import com.example.exchange.application.service.FinanceExportService;
import com.example.exchange.domain.model.dto.FinanceCategoryExportBatch;
import com.example.exchange.domain.model.dto.FinanceDailyReport;
import com.example.exchange.domain.model.dto.ReconciliationReportResult;
import com.example.exchange.domain.model.dto.RecoveryResult;
import com.example.exchange.domain.model.dto.TrialBalanceSnapshot;
import com.example.exchange.domain.model.dto.ValidationIssue;
import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.model.dto.OutboxDomainStateConsistencyReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.MatchingWorkerOwnerContextResponse;
import com.example.exchange.interfaces.web.dto.ReconciliationIssueActionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final AccountPositionConsistencyService accountPositionConsistencyService;
    private final ReconciliationService reconciliationService;
    private final ReconciliationReportService reconciliationReportService;
    private final ReconciliationIssueWorkflowService reconciliationIssueWorkflowService;
    private final MatchingWorkerLifecycleService matchingWorkerLifecycleService;
    private final WalletLedgerReplayService walletLedgerReplayService;
    private final FinanceReportService financeReportService;
    private final FinanceExportService financeExportService;
    private final LedgerArchiveEligibilityService ledgerArchiveEligibilityService;
    private final LedgerArchiveManifestService ledgerArchiveManifestService;
    private final TrialBalanceService trialBalanceService;
    private final OutboxService outboxService;
    private final OutboxDomainStateConsistencyService outboxDomainStateConsistencyService;

    public RecoveryController(
            SnapshotRecoverUseCase usecase,
            AccountPositionConsistencyService accountPositionConsistencyService,
            ReconciliationService reconciliationService,
            ReconciliationReportService reconciliationReportService,
            ReconciliationIssueWorkflowService reconciliationIssueWorkflowService,
            MatchingWorkerLifecycleService matchingWorkerLifecycleService,
            WalletLedgerReplayService walletLedgerReplayService,
            FinanceReportService financeReportService,
            FinanceExportService financeExportService,
            LedgerArchiveEligibilityService ledgerArchiveEligibilityService,
            LedgerArchiveManifestService ledgerArchiveManifestService,
            TrialBalanceService trialBalanceService,
            OutboxService outboxService,
            OutboxDomainStateConsistencyService outboxDomainStateConsistencyService
    ) {
        this.usecase = usecase;
        this.accountPositionConsistencyService = accountPositionConsistencyService;
        this.reconciliationService = reconciliationService;
        this.reconciliationReportService = reconciliationReportService;
        this.reconciliationIssueWorkflowService = reconciliationIssueWorkflowService;
        this.matchingWorkerLifecycleService = matchingWorkerLifecycleService;
        this.walletLedgerReplayService = walletLedgerReplayService;
        this.financeReportService = financeReportService;
        this.financeExportService = financeExportService;
        this.ledgerArchiveEligibilityService = ledgerArchiveEligibilityService;
        this.ledgerArchiveManifestService = ledgerArchiveManifestService;
        this.trialBalanceService = trialBalanceService;
        this.outboxService = outboxService;
        this.outboxDomainStateConsistencyService = outboxDomainStateConsistencyService;
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

    @GetMapping("/restore/account-position-consistency")
    public ApiResponse<AccountPositionConsistencyReport> accountPositionConsistency() {
        return ApiResponse.ok(accountPositionConsistencyService.validateAfterRestore());
    }

    @PostMapping("/reconcile/accounts/report")
    public ApiResponse<ReconciliationReportResult> createReconciliationReport(
            @RequestParam(defaultValue = "MANUAL") String triggeredBy
    ) {
        return ApiResponse.ok(reconciliationReportService.runAndPersist(triggeredBy));
    }

    @GetMapping("/reconcile/reports")
    public ApiResponse<List<ReconciliationReportResult>> latestReconciliationReports(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(reconciliationReportService.latest(limit));
    }

    @GetMapping("/reconcile/reports/{reportId}")
    public ApiResponse<ReconciliationReportResult> reconciliationReport(@PathVariable String reportId) {
        return ApiResponse.ok(reconciliationReportService.findById(reportId).orElse(null));
    }

    @GetMapping("/reconcile/ledger/{uid}/compare")
    public ApiResponse<LedgerReplayComparisonReport> compareLedgerReplay(
            @PathVariable long uid,
            @RequestParam(defaultValue = "USDT") String asset
    ) {
        return ApiResponse.ok(walletLedgerReplayService.compareAccountDetails(uid, asset));
    }

    @GetMapping("/reconcile/ledger/tamper-evidence")
    public ApiResponse<LedgerTamperEvidenceReport> verifyLedgerTamperEvidence() {
        return ApiResponse.ok(walletLedgerReplayService.verifyTamperEvidence());
    }

    @GetMapping("/finance/daily-report")
    public ApiResponse<FinanceDailyReport> financeDailyReport(
            @RequestParam String date
    ) {
        return ApiResponse.ok(financeReportService.dailyReport(LocalDate.parse(date)));
    }

    @GetMapping("/finance/category-report")
    public ApiResponse<FinanceDailyReport> financeCategoryReport(
            @RequestParam String date,
            @RequestParam(defaultValue = "fee") String category
    ) {
        return ApiResponse.ok(financeReportService.categoryReport(LocalDate.parse(date), category));
    }

    @GetMapping("/finance/category-export-batch")
    public ApiResponse<FinanceCategoryExportBatch> financeCategoryExportBatch(
            @RequestParam String date
    ) {
        return ApiResponse.ok(financeExportService.exportDailyCategories(LocalDate.parse(date)));
    }

    @GetMapping("/finance/ledger-archive-eligibility")
    public ApiResponse<LedgerArchiveEligibilityReport> ledgerArchiveEligibility(
            @RequestParam String date
    ) {
        return ApiResponse.ok(ledgerArchiveEligibilityService.evaluate(LocalDate.parse(date)));
    }

    @GetMapping("/finance/ledger-archive-manifest")
    public ApiResponse<LedgerArchiveManifest> ledgerArchiveManifest(
            @RequestParam String date
    ) {
        return ApiResponse.ok(ledgerArchiveManifestService.generate(LocalDate.parse(date)));
    }

    @GetMapping("/finance/ledger-archive-restore-smoke")
    public ApiResponse<LedgerArchiveRestoreSmokeReport> ledgerArchiveRestoreSmoke(
            @RequestParam String date
    ) {
        return ApiResponse.ok(ledgerArchiveManifestService.restoreSmoke(LocalDate.parse(date)));
    }

    @GetMapping("/finance/ledger-archive-replay-validation")
    public ApiResponse<LedgerArchiveReplayValidationReport> ledgerArchiveReplayValidation(
            @RequestParam String fromDate,
            @RequestParam String toDate
    ) {
        return ApiResponse.ok(ledgerArchiveManifestService.validateReplayRange(
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate)
        ));
    }

    @GetMapping("/finance/ledger-archive-delete-guard")
    public ApiResponse<LedgerArchiveDeleteGuardReport> ledgerArchiveDeleteGuard(
            @RequestParam String date
    ) {
        return ApiResponse.ok(ledgerArchiveManifestService.deleteGuard(LocalDate.parse(date)));
    }

    @PostMapping("/finance/trial-balance/snapshot")
    public ApiResponse<TrialBalanceSnapshot> persistTrialBalanceSnapshot(
            @RequestParam String date,
            @RequestParam Long uid,
            @RequestParam(defaultValue = "USDT") String asset
    ) {
        return ApiResponse.ok(trialBalanceService.persistSnapshot(LocalDate.parse(date), uid, asset));
    }

    @GetMapping("/finance/trial-balance/snapshot")
    public ApiResponse<TrialBalanceSnapshot> trialBalanceSnapshot(
            @RequestParam String date,
            @RequestParam Long uid,
            @RequestParam(defaultValue = "USDT") String asset
    ) {
        return ApiResponse.ok(trialBalanceService.snapshot(LocalDate.parse(date), uid, asset));
    }

    @GetMapping("/matching-worker/contexts")
    public ApiResponse<List<MatchingWorkerOwnerContextResponse>> matchingWorkerContexts() {
        return ApiResponse.ok(matchingWorkerLifecycleService.ownerContexts().values().stream()
                .map(MatchingWorkerOwnerContextResponse::from)
                .toList());
    }

    @GetMapping("/matching-worker/contexts/{symbol}")
    public ApiResponse<MatchingWorkerOwnerContextResponse> matchingWorkerContext(@PathVariable String symbol) {
        return ApiResponse.ok(matchingWorkerLifecycleService.ownerContext(symbol)
                .map(MatchingWorkerOwnerContextResponse::from)
                .orElse(null));
    }

    @GetMapping("/reconcile/issues/open")
    public ApiResponse<List<ReconciliationReportIssue>> openReconciliationIssues(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(reconciliationIssueWorkflowService.openIssues(limit));
    }

    @PostMapping("/reconcile/issues/{issueId}/claim")
    public ApiResponse<ReconciliationReportIssue> claimReconciliationIssue(
            @PathVariable long issueId,
            @Valid @RequestBody ReconciliationIssueActionRequest request
    ) {
        return ApiResponse.ok(reconciliationIssueWorkflowService.claim(issueId, request.owner()));
    }

    @PostMapping("/reconcile/issues/{issueId}/resolve")
    public ApiResponse<ReconciliationReportIssue> resolveReconciliationIssue(
            @PathVariable long issueId,
            @Valid @RequestBody ReconciliationIssueActionRequest request
    ) {
        return ApiResponse.ok(reconciliationIssueWorkflowService.resolve(issueId, request.owner()));
    }

    @PostMapping("/reconcile/issues/{issueId}/reopen")
    public ApiResponse<ReconciliationReportIssue> reopenReconciliationIssue(
            @PathVariable long issueId,
            @Valid @RequestBody ReconciliationIssueActionRequest request
    ) {
        return ApiResponse.ok(reconciliationIssueWorkflowService.reopen(issueId, request.owner()));
    }

    @GetMapping("/outbox/dlq")
    public ApiResponse<List<DlqEvent>> latestDlq(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(outboxService.latestDlq(limit));
    }

    @GetMapping("/outbox/domain-state-consistency")
    public ApiResponse<OutboxDomainStateConsistencyReport> outboxDomainStateConsistency(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(outboxDomainStateConsistencyService.inspectLatest(limit));
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
