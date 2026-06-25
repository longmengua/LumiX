/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.Account;
import com.example.exchange.domain.model.dto.WalletLedgerEntry;
import com.example.exchange.domain.model.dto.WalletLedgerPosting;
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
    public static final String USER_BONUS_AVAILABLE = "USER_BONUS_AVAILABLE";
    public static final String BONUS_PROMOTION_POOL = "BONUS_PROMOTION_POOL";
    public static final String BONUS_EXPIRY_EXPENSE = "BONUS_EXPIRY_EXPENSE";
    public static final String BONUS_CLAWBACK_RECEIVABLE = "BONUS_CLAWBACK_RECEIVABLE";

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
        account.deposit(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "deposit", refId, amount, account.balance(asset), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("EXTERNAL_CASH", asset, amount)
        ));
    }

    public void withdraw(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = accountRepo.findByUid(uid)
                .orElseThrow(() -> new IllegalStateException("account not found"));
        account.withdraw(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "withdrawal", refId, amount, account.balance(asset), List.of(
                debit("EXTERNAL_CASH", asset, amount),
                credit("USER_AVAILABLE", asset, amount)
        ));
    }

    public void reserveOrder(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.reserveOrder(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "order_reserve", refId, amount, account.balance(asset), List.of(
                debit("USER_ORDER_HOLD", asset, amount),
                credit("USER_AVAILABLE", asset, amount)
        ));
    }

    public void releaseOrderReserve(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.releaseOrderReserve(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "order_reserve_release", refId, amount, account.balance(asset), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("USER_ORDER_HOLD", asset, amount)
        ));
    }

    public BigDecimal increasePositionMargin(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return BigDecimal.ZERO;
        Account account = getOrCreate(uid);
        BigDecimal fromOrderHold = account.orderHold(asset).min(amount);
        BigDecimal fromAvailable = amount.subtract(fromOrderHold);
        if (fromOrderHold.signum() > 0) {
            account.convertOrderHoldToPositionMargin(asset, fromOrderHold);
        }
        if (fromAvailable.signum() > 0) {
            account.reservePositionMargin(asset, fromAvailable);
        }
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("USER_POSITION_MARGIN", asset, amount));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        append(uid, asset, "position_margin_increase", refId, amount, account.balance(asset), postings);
        return fromOrderHold;
    }

    public void releasePositionMargin(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.releasePositionMargin(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "position_margin_release", refId, amount, account.balance(asset), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("USER_POSITION_MARGIN", asset, amount)
        ));
    }

    public BigDecimal collectFee(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return BigDecimal.ZERO;
        Account account = getOrCreate(uid);
        BigDecimal fromOrderHold = account.orderHold(asset).min(amount);
        BigDecimal fromAvailable = amount.subtract(fromOrderHold);
        account.debitFromOrderHoldFirst(asset, amount);
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("USER_FEE_EXPENSE", asset, amount));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        append(uid, asset, "trade_fee", refId, amount, account.balance(asset), postings);
        return fromOrderHold;
    }

    public BigDecimal collectSpotFeeFromOrderHold(long uid, String asset, BigDecimal amount, String refId) {
        return collectFee(uid, asset, amount, refId);
    }

    public void settleSpotBuy(long uid, String baseAsset, String quoteAsset, BigDecimal baseQty, BigDecimal quoteNotional, String refId) {
        if (notPositive(baseQty) || notPositive(quoteNotional)) return;
        Account account = getOrCreate(uid);
        account.debitFromOrderHoldFirst(quoteAsset, quoteNotional);
        account.credit(baseAsset, baseQty);
        accountRepo.save(account);
        append(uid, quoteAsset, "spot_buy_quote_settlement", refId, quoteNotional, account.balance(quoteAsset), List.of(
                debit("SPOT_SETTLEMENT", quoteAsset, quoteNotional),
                credit("USER_ORDER_HOLD", quoteAsset, quoteNotional)
        ));
        append(uid, baseAsset, "spot_buy_base_delivery", refId, baseQty, account.balance(baseAsset), List.of(
                debit("USER_AVAILABLE", baseAsset, baseQty),
                credit("SPOT_SETTLEMENT", baseAsset, baseQty)
        ));
    }

    public void settleSpotSell(long uid, String baseAsset, String quoteAsset, BigDecimal baseQty, BigDecimal quoteNotional, String refId) {
        if (notPositive(baseQty) || notPositive(quoteNotional)) return;
        Account account = getOrCreate(uid);
        account.debitFromOrderHoldFirst(baseAsset, baseQty);
        account.credit(quoteAsset, quoteNotional);
        accountRepo.save(account);
        append(uid, baseAsset, "spot_sell_base_settlement", refId, baseQty, account.balance(baseAsset), List.of(
                debit("SPOT_SETTLEMENT", baseAsset, baseQty),
                credit("USER_ORDER_HOLD", baseAsset, baseQty)
        ));
        append(uid, quoteAsset, "spot_sell_quote_delivery", refId, quoteNotional, account.balance(quoteAsset), List.of(
                debit("USER_AVAILABLE", quoteAsset, quoteNotional),
                credit("SPOT_SETTLEMENT", quoteAsset, quoteNotional)
        ));
    }

    public void applyRealizedPnl(long uid, String asset, BigDecimal pnl, String refId) {
        if (pnl == null || pnl.signum() == 0) return;
        Account account = getOrCreate(uid);
        BigDecimal amount = pnl.abs();
        if (pnl.signum() > 0) {
            account.credit(asset, amount);
            accountRepo.save(account);
            append(uid, asset, "realized_pnl_profit", refId, amount, account.balance(asset), List.of(
                    debit("USER_AVAILABLE", asset, amount),
                    credit("REALIZED_PNL_INCOME", asset, amount)
            ));
            return;
        }

        BigDecimal fromAvailable = account.available(asset).min(amount);
        BigDecimal remaining = amount.subtract(fromAvailable);
        BigDecimal fromOrderHold = account.orderHold(asset).min(remaining);
        remaining = remaining.subtract(fromOrderHold);
        BigDecimal fromPositionMargin = remaining;
        account.debit(asset, amount);
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("REALIZED_PNL_LOSS", asset, amount));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromPositionMargin.signum() > 0) postings.add(credit("USER_POSITION_MARGIN", asset, fromPositionMargin));
        append(uid, asset, "realized_pnl_loss", refId, amount, account.balance(asset), postings);
    }

    public void applyFundingFee(long uid, String asset, BigDecimal cashflow, String refId) {
        if (cashflow == null || cashflow.signum() == 0) return;
        Account account = getOrCreate(uid);
        BigDecimal amount = cashflow.abs();

        if (cashflow.signum() > 0) {
            account.credit(asset, amount);
            accountRepo.save(account);
            append(uid, asset, "funding_fee_received", refId, amount, account.balance(asset), List.of(
                    debit("USER_AVAILABLE", asset, amount),
                    credit("FUNDING_TRANSFER", asset, amount)
            ));
            return;
        }

        BigDecimal fromAvailable = account.available(asset).min(amount);
        BigDecimal remaining = amount.subtract(fromAvailable);
        BigDecimal fromOrderHold = account.orderHold(asset).min(remaining);
        remaining = remaining.subtract(fromOrderHold);
        BigDecimal fromPositionMargin = remaining;
        account.debit(asset, amount);
        accountRepo.save(account);

        List<WalletLedgerPosting> postings = new ArrayList<>();
        postings.add(debit("FUNDING_FEE_EXPENSE", asset, amount));
        if (fromAvailable.signum() > 0) postings.add(credit("USER_AVAILABLE", asset, fromAvailable));
        if (fromOrderHold.signum() > 0) postings.add(credit("USER_ORDER_HOLD", asset, fromOrderHold));
        if (fromPositionMargin.signum() > 0) postings.add(credit("USER_POSITION_MARGIN", asset, fromPositionMargin));
        append(uid, asset, "funding_fee_paid", refId, amount, account.balance(asset), postings);
    }

    public void applyInsurancePayout(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.credit(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "insurance_fund_payout", refId, amount, account.balance(asset), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("INSURANCE_FUND", asset, amount)
        ));
    }

    public void applyAdlCompensation(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.credit(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "adl_socialized_loss", refId, amount, account.balance(asset), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("ADL_SOCIALIZED_LOSS", asset, amount)
        ));
    }

    public void applyAdlForcedLoss(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.debit(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "adl_forced_loss", refId, amount, account.balance(asset), List.of(
                debit("ADL_SOCIALIZED_LOSS", asset, amount),
                credit("USER_AVAILABLE", asset, amount)
        ));
    }

    public void creditRebate(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        Account account = getOrCreate(uid);
        account.credit(asset, amount);
        accountRepo.save(account);
        append(uid, asset, "trade_rebate", refId, amount, account.balance(asset), List.of(
                debit("USER_AVAILABLE", asset, amount),
                credit("REBATE_EXPENSE", asset, amount)
        ));
    }

    public void grantBonusCredit(long uid, String asset, BigDecimal amount, String refId) {
        if (notPositive(amount)) return;
        // Bonus credit is intentionally excluded from Account.crossBalance so it cannot mix with real cash.
        append(uid, asset, "bonus_credit_grant", refId, amount, bonusCreditBalance(uid, asset).add(amount), List.of(
                debit(USER_BONUS_AVAILABLE, asset, amount),
                credit(BONUS_PROMOTION_POOL, asset, amount)
        ));
    }

    public BigDecimal consumeBonusCredit(long uid, String asset, BigDecimal amount, String refId, String expenseAccount) {
        if (notPositive(amount)) return BigDecimal.ZERO;
        BigDecimal available = bonusCreditBalance(uid, asset);
        BigDecimal consumed = available.min(amount);
        if (consumed.signum() <= 0) return BigDecimal.ZERO;
        String debitAccount = expenseAccount == null || expenseAccount.isBlank()
                ? "USER_FEE_EXPENSE"
                : expenseAccount.trim();
        append(uid, asset, "bonus_credit_consume", refId, consumed, available.subtract(consumed), List.of(
                debit(debitAccount, asset, consumed),
                credit(USER_BONUS_AVAILABLE, asset, consumed)
        ));
        return consumed;
    }

    public BigDecimal expireBonusCredit(long uid, String asset, BigDecimal amount, String refId) {
        return removeBonusCredit(uid, asset, amount, refId, "bonus_credit_expire", BONUS_EXPIRY_EXPENSE);
    }

    public BigDecimal clawbackBonusCredit(long uid, String asset, BigDecimal amount, String refId) {
        return removeBonusCredit(uid, asset, amount, refId, "bonus_credit_clawback", BONUS_CLAWBACK_RECEIVABLE);
    }

    public BigDecimal bonusCreditBalance(long uid, String asset) {
        List<WalletLedgerEntry> entries = ledgerJournal == null
                ? ledgerRepo.findByUid(uid)
                : ledgerJournal.findByUidAndAsset(uid, asset);
        BigDecimal balance = BigDecimal.ZERO;
        for (WalletLedgerEntry entry : entries) {
            if (!asset.equals(entry.getAsset()) || entry.getPostings() == null) continue;
            for (WalletLedgerPosting posting : entry.getPostings()) {
                if (!USER_BONUS_AVAILABLE.equals(posting.accountCode())) continue;
                balance = balance.add(posting.debit()).subtract(posting.credit());
            }
        }
        return balance;
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

    private BigDecimal removeBonusCredit(
            long uid,
            String asset,
            BigDecimal amount,
            String refId,
            String reason,
            String debitAccount
    ) {
        if (notPositive(amount)) return BigDecimal.ZERO;
        BigDecimal available = bonusCreditBalance(uid, asset);
        BigDecimal removed = available.min(amount);
        if (removed.signum() <= 0) return BigDecimal.ZERO;
        append(uid, asset, reason, refId, removed, available.subtract(removed), List.of(
                debit(debitAccount, asset, removed),
                credit(USER_BONUS_AVAILABLE, asset, removed)
        ));
        return removed;
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
