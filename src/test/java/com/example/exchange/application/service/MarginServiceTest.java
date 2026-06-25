package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.dto.TransferReconciliationProjection;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.domain.repository.WalletTransferRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 MarginService 的入金/出金狀態機與 ledger side effect。
 *
 * <p>這裡不測 HTTP controller；只驗證 service 在成功、風控暫停、
 * 餘額不足三種核心情境下的 transfer 狀態與帳務是否一致。</p>
 */
class MarginServiceTest {

    @Test
    @DisplayName("入金與成功出金會建立 transfer 並寫入 ledger")
    /**
     * 流程：建立未暫停風控 -> deposit 再 withdraw -> 驗證 transfer、帳戶餘額與 ledger 順序。
     */
    void depositAndWithdrawCreateTransfersAndLedgerEntries() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        WalletTransfer deposit = fixtures.marginService.deposit(11, new BigDecimal("100.00"));
        WalletTransfer withdrawal = fixtures.marginService.withdraw(11, new BigDecimal("40.00"));

        Account account = fixtures.accountRepository.findByUid(11).orElseThrow();
        assertThat(deposit.getStatus()).isEqualTo(WalletTransfer.Status.CONFIRMED);
        assertThat(withdrawal.getStatus()).isEqualTo(WalletTransfer.Status.CONFIRMED);
        assertThat(account.crossBalance()).isEqualByComparingTo("60.00");
        assertThat(account.crossAvailable()).isEqualByComparingTo("60.00");
        assertThat(fixtures.ledgerRepository.findByUid(11))
                .extracting(WalletLedgerEntry::getReason)
                .containsExactly("deposit", "withdrawal");
        assertThat(fixtures.transferRepository.findByUid(11))
                .extracting(WalletTransfer::getType)
                .containsExactly(WalletTransfer.Type.DEPOSIT, WalletTransfer.Type.WITHDRAWAL);
    }

    @Test
    @DisplayName("指定資產入金與出金會依資產維度更新可用餘額")
    /**
     * 流程：先對 BTC 入金，再對 BTC 出金。
     * 期望：transfer 與 ledger 都保留 BTC 資產欄位，且帳戶 BTC 可用額正確變化。
     */
    void assetSpecificDepositAndWithdrawUpdateAssetBalance() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        WalletTransfer deposit = fixtures.marginService.deposit(16, "btc", new BigDecimal("2.5000"));
        WalletTransfer withdrawal = fixtures.marginService.withdraw(16, "BTC", new BigDecimal("1.2500"));

        Account account = fixtures.accountRepository.findByUid(16).orElseThrow();
        assertThat(deposit.getAsset()).isEqualTo("BTC");
        assertThat(withdrawal.getAsset()).isEqualTo("BTC");
        assertThat(account.available("BTC")).isEqualByComparingTo("1.2500");
        assertThat(account.available("USDT")).isEqualByComparingTo("0");
        assertThat(fixtures.ledgerRepository.findByUid(16))
                .extracting(WalletLedgerEntry::getAsset)
                .containsExactly("BTC", "BTC");
    }

    @Test
    @DisplayName("出金暫停時進入人工覆核且不扣款")
    /**
     * 流程：先入金建立餘額 -> 開啟 withdrawal halt -> withdraw 進人工覆核且帳戶不扣款。
     */
    void withdrawalHaltRoutesToManualReviewWithoutDebiting() {
        RiskControlsProperties riskControls = new RiskControlsProperties();
        Fixtures fixtures = fixtures(riskControls);
        fixtures.marginService.deposit(12, new BigDecimal("100.00"));
        riskControls.setWithdrawalHalt(true);

        WalletTransfer withdrawal = fixtures.marginService.withdraw(12, new BigDecimal("25.00"));

        Account account = fixtures.accountRepository.findByUid(12).orElseThrow();
        assertThat(withdrawal.getStatus()).isEqualTo(WalletTransfer.Status.MANUAL_REVIEW);
        assertThat(withdrawal.getReason()).isEqualTo("WITHDRAWAL_HALTED");
        assertThat(account.crossBalance()).isEqualByComparingTo("100.00");
        assertThat(fixtures.ledgerRepository.findByUid(12))
                .extracting(WalletLedgerEntry::getReason)
                .containsExactly("deposit");
    }

    @Test
    @DisplayName("餘額不足時出金失敗且不建立 ledger entry")
    /**
     * 流程：不先建立帳戶餘額 -> 直接 withdraw -> 驗證 transfer failed 且沒有 account/ledger side effect。
     */
    void insufficientBalanceCreatesFailedWithdrawalWithoutLedgerEntry() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        WalletTransfer withdrawal = fixtures.marginService.withdraw(13, new BigDecimal("10.00"));

        assertThat(withdrawal.getStatus()).isEqualTo(WalletTransfer.Status.FAILED);
        assertThat(withdrawal.getReason()).isEqualTo("INSUFFICIENT_AVAILABLE_BALANCE");
        assertThat(fixtures.accountRepository.findByUid(13)).isEmpty();
        assertThat(fixtures.ledgerRepository.findByUid(13)).isEmpty();
        assertThat(fixtures.transferRepository.findByUid(13)).containsExactly(withdrawal);
    }

    @Test
    @DisplayName("入金 callback 使用 externalRef 冪等 replay，避免重複入帳")
    /**
     * 流程：鏈上 / 銀行 callback 以同一 externalRef 重送兩次。
     * 期望：第二次回傳既有 transfer，不再新增 ledger；若同 ref payload 不同則拒絕。
     */
    void depositCallbackReplaysByExternalRef() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        WalletTransfer first =
                fixtures.marginService.recordDepositCallback(
                        14,
                        new BigDecimal("50.00"),
                        "bank-tx-1"
                );
        WalletTransfer replay =
                fixtures.marginService.recordDepositCallback(
                        14,
                        new BigDecimal("50.00"),
                        "bank-tx-1"
                );

        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(replay.getExternalRef()).isEqualTo("bank-tx-1");
        assertThat(fixtures.ledgerRepository.findByUid(14))
                .hasSize(1);
        assertThat(fixtures.accountRepository.findByUid(14).orElseThrow().crossBalance())
                .isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("入金 callback 若同 externalRef 對應不同資產則拒絕")
    /**
     * 流程：先用 BTC callback 建立 transfer，再以同一 externalRef 傳入 USDT。
     * 期望：service 視為外部流水號衝突，避免不同幣種被重放成同一筆入金。
     */
    void depositCallbackRejectsAssetMismatchOnSameExternalRef() {
        Fixtures fixtures = fixtures(new RiskControlsProperties());

        fixtures.marginService.recordDepositCallback(
                17,
                "BTC",
                new BigDecimal("0.5000"),
                "chain-tx-1"
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                fixtures.marginService.recordDepositCallback(
                        17,
                        "USDT",
                        new BigDecimal("0.5000"),
                        "chain-tx-1"
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deposit callback conflict");
    }

    @Test
    @DisplayName("人工覆核 transfer 可被 owner claim，且 reconciliation projection 顯示未入帳")
    /**
     * 流程：出金暫停產生 MANUAL_REVIEW transfer，operator claim 後查 reconciliation projection。
     * 期望：owner 被記錄；manual review 未寫 ledger 仍視為 matched，避免誤判漏帳。
     */
    void manualReviewClaimAndTransferReconciliationProjection() {
        RiskControlsProperties riskControls = new RiskControlsProperties();
        Fixtures fixtures = fixtures(riskControls);
        fixtures.marginService.deposit(15, new BigDecimal("100.00"));
        riskControls.setWithdrawalHalt(true);
        WalletTransfer review =
                fixtures.marginService.withdraw(15, new BigDecimal("30.00"));

        WalletTransfer claimed =
                fixtures.marginService.claimManualReview(review.getId(), "ops-1");
        List<TransferReconciliationProjection> projections =
                fixtures.marginService.transferReconciliation(15);

        assertThat(claimed.getReviewOwner()).isEqualTo("ops-1");
        assertThat(projections)
                .extracting(TransferReconciliationProjection::ledgerMatched)
                .containsExactly(true, true);
        assertThat(projections.get(1).status())
                .isEqualTo(WalletTransfer.Status.MANUAL_REVIEW);
        assertThat(projections.get(1).ledgerEntryCount())
                .isZero();
        assertThat(projections.get(1).reviewOwner())
                .isEqualTo("ops-1");
    }

    /**
     * 建立 MarginService 的測試鏈路，串起 account repository、ledger service、transfer repository 與風控設定。
     */
    private static Fixtures fixtures(RiskControlsProperties riskControlsProperties) {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        MemWalletTransferRepository transferRepository = new MemWalletTransferRepository();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepository, ledgerRepository);
        MarginService marginService = new MarginService(
                accountRepository,
                walletLedgerService,
                ledgerRepository,
                transferRepository,
                riskControlsProperties
        );
        return new Fixtures(accountRepository, ledgerRepository, transferRepository, marginService);
    }

    private record Fixtures(
            MemAccountRepository accountRepository,
            MemWalletLedgerRepository ledgerRepository,
            MemWalletTransferRepository transferRepository,
            MarginService marginService
    ) {
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        /**
         * 依 uid 讀帳戶，讓 deposit/withdraw 能取得現有資金狀態。
         */
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        /**
         * 寫回帳戶餘額變化，供後續 assertion 檢查 cross balance/available。
         */
        public void save(Account account) {
            accounts.put(account.uid(), account);
        }
    }

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        /**
         * 接收 ledger entry，並在測試 stub 內先確認每筆分錄都是 balanced。
         */
        public void append(WalletLedgerEntry entry) {
            assertThat(entry.isBalanced()).isTrue();
            entries.add(entry);
        }

        @Override
        /**
         * 依 uid 查 ledger，驗證 deposit/withdraw 是否產生預期帳務原因。
         */
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream()
                    .filter(entry -> entry.getUid() == uid)
                    .toList();
        }

        @Override
        /**
         * 依 refId 查 ledger；目前測試不主查 refId，但保留 repository contract。
         */
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream()
                    .filter(entry -> refId.equals(entry.getRefId()))
                    .toList();
        }
    }

    private static class MemWalletTransferRepository implements WalletTransferRepository {
        private final Map<UUID, WalletTransfer> transfers = new LinkedHashMap<>();

        @Override
        /**
         * 保存每筆 transfer，讓測試能確認入金/出金狀態機的落點。
         */
        public void save(WalletTransfer transfer) {
            transfers.put(transfer.getId(), transfer);
        }

        @Override
        /**
         * 依 transfer id 查單筆資料，保留 service 可能使用的查詢入口。
         */
        public Optional<WalletTransfer> findById(UUID id) {
            return Optional.ofNullable(transfers.get(id));
        }

        @Override
        /**
         * 依 externalRef 查 callback-created transfer，模擬 production callback 去重索引。
         */
        public Optional<WalletTransfer> findByExternalRef(String externalRef) {
            return transfers.values().stream()
                    .filter(transfer -> externalRef.equals(transfer.getExternalRef()))
                    .findFirst();
        }

        @Override
        /**
         * 依 uid 回傳 transfer 歷史，用來驗證 deposit 與 withdrawal 建立順序。
         */
        public List<WalletTransfer> findByUid(long uid) {
            return transfers.values().stream()
                    .filter(transfer -> transfer.getUid() == uid)
                    .toList();
        }
    }
}
