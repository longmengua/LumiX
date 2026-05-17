/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
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
        requirePositive(amount, "deposit amount");
        WalletTransfer transfer = WalletTransfer.builder()
                .uid(uid)
                .asset(DEFAULT_ASSET)
                .amount(amount)
                .type(WalletTransfer.Type.DEPOSIT)
                .build();
        walletTransferRepository.save(transfer);

        walletLedgerService.deposit(uid, DEFAULT_ASSET, amount, transfer.getId().toString());
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
        requirePositive(amount, "withdrawal amount");
        WalletTransfer transfer = WalletTransfer.builder()
                .uid(uid)
                .asset(DEFAULT_ASSET)
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
        if (account == null || account.crossAvailable().compareTo(amount) < 0) {
            transfer.fail("INSUFFICIENT_AVAILABLE_BALANCE");
            walletTransferRepository.save(transfer);
            return transfer;
        }

        walletLedgerService.withdraw(uid, DEFAULT_ASSET, amount, transfer.getId().toString());
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
}
