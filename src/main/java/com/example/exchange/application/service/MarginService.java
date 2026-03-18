package com.example.exchange.application.service;

import com.example.exchange.domain.model.Account;
import com.example.exchange.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 保證金相關服務
 * - 封裝 Cross/Isolated 劃轉
 */
@Service
@RequiredArgsConstructor
public class MarginService {

    private final AccountRepository accountRepo;

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
