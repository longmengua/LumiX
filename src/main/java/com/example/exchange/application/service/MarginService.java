package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 保證金相關服務
 * - 封裝 Cross/Isolated 劃轉
 */
@Service
@RequiredArgsConstructor
public class MarginService {

    private final AccountRepository accountRepo;
    private final WalletLedgerService walletLedgerService;
    private final WalletLedgerRepository walletLedgerRepository;

    public void deposit(long uid, BigDecimal amount) {
        walletLedgerService.deposit(uid, "USDT", amount, "manual-deposit-" + System.nanoTime());
    }

    public Optional<Account> findAccount(long uid) {
        return accountRepo.findByUid(uid);
    }

    public List<WalletLedgerEntry> findLedger(long uid) {
        return walletLedgerRepository.findByUid(uid);
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
}
