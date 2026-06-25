/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.dto.TransferReconciliationProjection;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.domain.repository.WalletTransferRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 保證金相關服務
 * - 封裝入金、出金狀態機與 Cross/Isolated 劃轉
 * - 入金/出金會先寫 WalletTransfer，再更新帳務 ledger，最後轉成終態
 */
@Service
@RequiredArgsConstructor
public class MarginService {

    private static final String DEFAULT_ASSET = "USDT";

    private final AccountRepository accountRepo;
    private final WalletLedgerService walletLedgerService;
    private final WalletLedgerRepository walletLedgerRepository;
    private final WalletTransferRepository walletTransferRepository;
    private final RiskControlsProperties riskControlsProperties;

    /**
     * 建立入金 transfer 並立即確認。
     *
     * <p>MVP 使用人工入金語意，呼叫成功代表可用餘額已增加並寫入 ledger。
     * 日後接鏈上或銀行 callback 時，這裡可改成 pending -> confirmed 的非同步流程。</p>
     */
    public WalletTransfer deposit(long uid, BigDecimal amount) {
        return deposit(uid, null, amount);
    }

    /**
     * 建立指定資產入金 transfer 並立即確認。
     *
     * <p>現貨帳務需要 base/quote 多資產餘額，這裡保留 asset 可選參數，
     * 未傳時仍回落到既有 USDT 預設，避免舊 caller 失效。</p>
     */
    public WalletTransfer deposit(long uid, String asset, BigDecimal amount) {
        requirePositive(amount, "deposit amount");
        return createConfirmedDeposit(uid, asset, amount, null);
    }

    /**
     * 處理鏈上 / 銀行入金 callback。
     *
     * <p>`externalRef` 是外部流水號或 tx hash；相同 externalRef 重送會 replay 既有 transfer，
     * 不會再次寫入 ledger。若 externalRef 被不同 uid/amount/type 重用，會拒絕為 callback conflict。</p>
     */
    public WalletTransfer recordDepositCallback(
            long uid,
            BigDecimal amount,
            String externalRef
    ) {
        return recordDepositCallback(uid, null, amount, externalRef);
    }

    /**
     * 處理指定資產的鏈上 / 銀行入金 callback。
     *
     * <p>callback 需要把 externalRef 與資產一起視為不可變 payload，
     * 否則不同幣種共用同一外部流水號時會錯誤 replay 到舊 transfer。</p>
     */
    public WalletTransfer recordDepositCallback(
            long uid,
            String asset,
            BigDecimal amount,
            String externalRef
    ) {
        requirePositive(amount, "deposit amount");
        requireExternalRef(externalRef);
        String normalizedAsset = normalizeAsset(asset);

        Optional<WalletTransfer> existing =
                walletTransferRepository.findByExternalRef(externalRef.trim());
        if (existing.isPresent()) {
            WalletTransfer transfer =
                    existing.get();
            assertSameDepositCallback(transfer, uid, normalizedAsset, amount);
            return transfer;
        }

        return createConfirmedDeposit(uid, normalizedAsset, amount, externalRef.trim());
    }

    private WalletTransfer createConfirmedDeposit(
            long uid,
            String asset,
            BigDecimal amount,
            String externalRef
    ) {
        String normalizedAsset = normalizeAsset(asset);
        WalletTransfer transfer = WalletTransfer.builder()
                .uid(uid)
                .asset(normalizedAsset)
                .amount(amount)
                .type(WalletTransfer.Type.DEPOSIT)
                .externalRef(externalRef)
                .build();
        walletTransferRepository.save(transfer);

        walletLedgerService.deposit(uid, normalizedAsset, amount, transfer.getId().toString());
        transfer.confirm();
        walletTransferRepository.save(transfer);
        return transfer;
    }

    /**
     * 建立出金 transfer。
     *
     * <p>若 withdrawal halt 開啟，transfer 會進入 MANUAL_REVIEW 且不扣款；
     * 若餘額不足，transfer 會進入 FAILED 且不寫 ledger。</p>
     */
    public WalletTransfer withdraw(long uid, BigDecimal amount) {
        return withdraw(uid, null, amount);
    }

    /**
     * 建立指定資產出金 transfer。
     *
     * <p>現貨需要依資產維度檢查 available，否則 BTC 現貨賣方會被錯誤套用 USDT 可用額度。</p>
     */
    public WalletTransfer withdraw(long uid, String asset, BigDecimal amount) {
        requirePositive(amount, "withdrawal amount");
        String normalizedAsset = normalizeAsset(asset);
        WalletTransfer transfer = WalletTransfer.builder()
                .uid(uid)
                .asset(normalizedAsset)
                .amount(amount)
                .type(WalletTransfer.Type.WITHDRAWAL)
                .build();
        walletTransferRepository.save(transfer);

        if (riskControlsProperties.isWithdrawalHalt()) {
            transfer.markManualReview("WITHDRAWAL_HALTED");
            walletTransferRepository.save(transfer);
            return transfer;
        }

        Account account = accountRepo.findByUid(uid).orElse(null);
        if (account == null || account.available(normalizedAsset).compareTo(amount) < 0) {
            transfer.fail("INSUFFICIENT_AVAILABLE_BALANCE");
            walletTransferRepository.save(transfer);
            return transfer;
        }

        walletLedgerService.withdraw(uid, normalizedAsset, amount, transfer.getId().toString());
        transfer.confirm();
        walletTransferRepository.save(transfer);
        return transfer;
    }

    /** 查詢帳戶熱狀態；不存在時回傳 Optional.empty()。 */
    public Optional<Account> findAccount(long uid) {
        return accountRepo.findByUid(uid);
    }

    /** 查詢使用者 ledger entries，供 API 與對帳工具檢視。 */
    public List<WalletLedgerEntry> findLedger(long uid) {
        return walletLedgerRepository.findByUid(uid);
    }

    /** 查詢入金/出金 transfer state machine 歷史。 */
    public List<WalletTransfer> findTransfers(long uid) {
        return walletTransferRepository.findByUid(uid);
    }

    /** 指派人工覆核 owner，避免多位 operator 同時處理同一筆 transfer。 */
    public WalletTransfer claimManualReview(
            UUID transferId,
            String owner
    ) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner is required");
        }
        WalletTransfer transfer =
                walletTransferRepository.findById(transferId)
                        .orElseThrow(() -> new IllegalArgumentException("transfer not found"));
        transfer.claimManualReview(owner.trim());
        walletTransferRepository.save(transfer);
        return transfer;
    }

    /** 建立 transfer 對 ledger 的 reconciliation projection，供營運查漏帳、重複入帳與 manual-review queue。 */
    public List<TransferReconciliationProjection> transferReconciliation(long uid) {
        return walletTransferRepository.findByUid(uid)
                .stream()
                .map(this::toProjection)
                .toList();
    }

    /**
     * 劃轉：Cross <-> Isolated
     * - 若帳戶不存在則建立（簡化示範）
     */
    public void transferIsolated(long uid, String symbol, boolean toIsolated, BigDecimal amount) {
        Account acc = accountRepo.findByUid(uid).orElseGet(() -> {
            Account a = new Account(uid);
            accountRepo.save(a);
            return a;
        });

        if (toIsolated) {
            acc.moveToIsolated(symbol, amount);
        } else {
            acc.moveFromIsolated(symbol, amount);
        }

        accountRepo.save(acc);
    }

    private static void requirePositive(BigDecimal amount, String label) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }

    private TransferReconciliationProjection toProjection(WalletTransfer transfer) {
        List<WalletLedgerEntry> entries =
                walletLedgerRepository.findByRefId(transfer.getId().toString());
        BigDecimal ledgerAmount =
                entries.stream()
                        .map(WalletLedgerEntry::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean expectedLedger =
                transfer.getStatus() == WalletTransfer.Status.CONFIRMED;
        boolean ledgerMatched =
                expectedLedger
                        ? ledgerAmount.compareTo(transfer.getAmount()) == 0
                        : ledgerAmount.compareTo(BigDecimal.ZERO) == 0;

        return new TransferReconciliationProjection(
                transfer.getId(),
                transfer.getUid(),
                transfer.getType(),
                transfer.getStatus(),
                transfer.getAsset(),
                transfer.getAmount(),
                entries.size(),
                ledgerAmount,
                ledgerMatched,
                transfer.getExternalRef(),
                transfer.getReviewOwner(),
                transfer.getReason()
        );
    }

    private void assertSameDepositCallback(
            WalletTransfer transfer,
            long uid,
            String asset,
            BigDecimal amount
    ) {
        if (transfer.getType() != WalletTransfer.Type.DEPOSIT
                || transfer.getUid() != uid
                || !normalizeAsset(transfer.getAsset()).equals(asset)
                || transfer.getAmount().compareTo(amount) != 0) {
            throw new IllegalStateException(
                    "deposit callback conflict: externalRef="
                            + transfer.getExternalRef()
            );
        }
    }

    private static void requireExternalRef(String externalRef) {
        if (externalRef == null || externalRef.isBlank()) {
            throw new IllegalArgumentException("externalRef is required");
        }
    }

    private static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            return DEFAULT_ASSET;
        }
        return asset.trim().toUpperCase();
    }
}
