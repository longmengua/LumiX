/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletLedgerService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountRepository accountRepo;
    private final WalletLedgerRepository ledgerRepo;
    private WalletLedgerJournal ledgerJournal;

    @Autowired(required = false)
    public void setLedgerJournal(WalletLedgerJournal ledgerJournal) {
        this.ledgerJournal = ledgerJournal;
    }

    public Account getOrCreate(long uid) {
        return accountRepo.findByUid(uid).orElseGet(() -> {
            Account account = new Account(uid);
            accountRepo.save(account);
            return account;
        });
    }

    public void deposit(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.deposit(amount);
        accountRepo.save(account);
        append(uid, asset, "deposit", refId, amount, account.crossBalance(), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("EXTERNAL_CASH", asset, amount)
        ));
    }

    public void withdraw(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = accountRepo.findByUid(uid)
                .orElseThrow(() -> new IllegalStateException("account not found"));
        account.withdraw(amount);
        accountRepo.save(account);
        append(uid, asset, "withdrawal", refId, amount, account.crossBalance(), List.of(
                debit("EXTERNAL_CASH", asset, amount),
                credit("USER_AVAILABLE", asset, amount)
        ));
    }

    public void reserveOrder(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.reserveOrder(amount);
        accountRepo.save(account);
        append(uid, asset, "order_reserve", refId, amount, account.crossBalance(), List.of(
                debit("USER_ORDER_HOLD", asset, amount),
                credit("USER_AVAILABLE", asset, amount)
        ));
    }

    public void releaseOrderReserve(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.releaseOrderReserve(amount);
        accountRepo.save(account);
        append(uid, asset, "order_reserve_release", refId, amount, account.crossBalance(), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("USER_ORDER_HOLD", asset, amount)
        ));
    }

    public BigDecimal increasePositionMargin(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return BigDecimal.ZERO;
        Account account = getOrCreate(uid);
        BigDecimal fromOrderHold = account.crossOrderHold().min(amount);
        BigDecimal fromAvailable = amount.subtract(fromOrderHold);
        if (fromOrderHold.signum() > 0) {
            account.convertOrderHoldToPositionMargin(fromOrderHold);
        }
        if (fromAvailable.signum() > 0) {
            account.reservePositionMargin(fromAvailable);
        }
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("USER_POSITION_MARGIN", asset, amount));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        append(uid, asset, "position_margin_increase", refId, amount, account.crossBalance(), postings);
        return fromOrderHold;
    }

    public void releasePositionMargin(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.releasePositionMargin(amount);
        accountRepo.save(account);
        append(uid, asset, "position_margin_release", refId, amount, account.crossBalance(), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("USER_POSITION_MARGIN", asset, amount)
        ));
    }

    public BigDecimal collectFee(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return BigDecimal.ZERO;
        Account account = getOrCreate(uid);
        BigDecimal fromOrderHold = account.crossOrderHold().min(amount);
        BigDecimal fromAvailable = amount.subtract(fromOrderHold);
        account.debitFromOrderHoldFirst(amount);
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("USER_FEE_EXPENSE", asset, amount));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        append(uid, asset, "trade_fee", refId, amount, account.crossBalance(), postings);
        return fromOrderHold;
    }

    public void applyRealizedPnl(long uid, String asset, BigDecimal pnl, String refId) {
        if (pnl == null || pnl.signum() == 0) return;
        Account account = getOrCreate(uid);
        BigDecimal amount = pnl.abs();
        if (pnl.signum() > 0) {
            account.credit(amount);
            accountRepo.save(account);
            append(uid, asset, "realized_pnl_profit", refId, amount, account.crossBalance(), List.of(
                    debit("USER_AVAILABLE", asset, amount),
                    credit("REALIZED_PNL_INCOME", asset, amount)
            ));
            return;
        }

        BigDecimal fromAvailable = account.crossAvailable().min(amount);
        BigDecimal remaining = amount.subtract(fromAvailable);
        BigDecimal fromOrderHold = account.crossOrderHold().min(remaining);
        remaining = remaining.subtract(fromOrderHold);
        BigDecimal fromPositionMargin = remaining;
        account.debit(amount);
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("REALIZED_PNL_LOSS", asset, amount));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromPositionMargin.signum() > 0) postings.add(credit("USER_POSITION_MARGIN", asset, fromPositionMargin));
        append(uid, asset, "realized_pnl_loss", refId, amount, account.crossBalance(), postings);
    }

    public void applyFundingFee(long uid, String asset, BigDecimal cashflow, String refId) {
        if (cashflow == null || cashflow.signum() == 0) return;
        Account account = getOrCreate(uid);
        BigDecimal amount = cashflow.abs();

        if (cashflow.signum() > 0) {
            account.credit(amount);
            accountRepo.save(account);
            append(uid, asset, "funding_fee_received", refId, amount, account.crossBalance(), List.of(
                    debit("USER_AVAILABLE", asset, amount),
                    credit("FUNDING_TRANSFER", asset, amount)
            ));
            return;
        }

        BigDecimal fromAvailable = account.crossAvailable().min(amount);
        BigDecimal remaining = amount.subtract(fromAvailable);
        BigDecimal fromOrderHold = account.crossOrderHold().min(remaining);
        remaining = remaining.subtract(fromOrderHold);
        BigDecimal fromPositionMargin = remaining;
        account.debit(amount);
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("FUNDING_FEE_EXPENSE", asset, amount));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromPositionMargin.signum() > 0) postings.add(credit("USER_POSITION_MARGIN", asset, fromPositionMargin));
        append(uid, asset, "funding_fee_paid", refId, amount, account.crossBalance(), postings);
    }

    public void applyInsurancePayout(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.credit(amount);
        accountRepo.save(account);
        append(uid, asset, "insurance_fund_payout", refId, amount, account.crossBalance(), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("INSURANCE_FUND", asset, amount)
        ));
    }

    public void applyAdlCompensation(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.credit(amount);
        accountRepo.save(account);
        append(uid, asset, "adl_socialized_loss", refId, amount, account.crossBalance(), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("ADL_SOCIALIZED_LOSS", asset, amount)
        ));
    }

    public void creditRebate(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.credit(amount);
        accountRepo.save(account);
        append(uid, asset, "trade_rebate", refId, amount, account.crossBalance(), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("REBATE_EXPENSE", asset, amount)
        ));
    }

    private void append(
            long uid,
            String asset,
            String reason,
            String refId,
            BigDecimal amount,
            BigDecimal balanceAfter,
            List<WalletLedgerPosting> postings
    ) {
        WalletLedgerEntry entry = WalletLedgerEntry.builder()
                .uid(uid)
                .asset(asset)
                .reason(reason)
                .refId(refId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .postings(postings)
                .build();
        if (ledgerJournal != null) {
            ledgerJournal.append(entry);
        }
        ledgerRepo.append(entry);
    }

    private static boolean notPositive(BigDecimal amount) {
        return amount == null || amount.signum() <= 0;
    }

    private static WalletLedgerPosting debit(String accountCode, String asset, BigDecimal amount) {
        return new WalletLedgerPosting(accountCode, asset, amount, ZERO);
    }

    private static WalletLedgerPosting credit(String accountCode, String asset, BigDecimal amount) {
        return new WalletLedgerPosting(accountCode, asset, ZERO, amount);
    }
}
