package com.lumix.trading.runtime;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.runtime.LedgerPostingActor;
import com.lumix.ledger.runtime.LedgerPostingExecutionCommand;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 逐倉 funding 的週期性 USDT 帳本結算；每個週期只能提交一次。 */
@Service
@Profile("infrastructure")
public class FuturesFundingSettlementService {
    private final JdbcTemplate jdbc;
    private final FuturesRiskPolicyRepository policies;
    private final TradingExecutionSupport support;
    private final TransactionalLedgerPostingService ledger;
    private final ObjectProvider<BinanceFuturesReferencePriceFeed> references;
    private final SuperAdminAccessService access;

    public FuturesFundingSettlementService(JdbcTemplate jdbc, FuturesRiskPolicyRepository policies,
            TradingExecutionSupport support, TransactionalLedgerPostingService ledger,
            ObjectProvider<BinanceFuturesReferencePriceFeed> references, SuperAdminAccessService access) {
        this.jdbc = jdbc;
        this.policies = policies;
        this.support = support;
        this.ledger = ledger;
        this.references = references;
        this.access = access;
    }

    public record Result(String cycleId, String instrumentId, Instant periodEnd, String fundingRate,
                         String markPrice, Long ledgerJournalId, int positionCount) { }

    /** 費率由最高管理員逐期核定；無具名行情、未到期或不在政策時鐘上即拒絕。 */
    @Transactional
    public Result settle(AuthenticatedUser actor, String market, Instant periodEnd, BigDecimal rate) {
        access.requireActiveSuperAdmin(actor);
        Objects.requireNonNull(periodEnd, "funding period end is required");
        Objects.requireNonNull(rate, "funding rate is required");
        TradingDecimal.ledgerAmount(rate.abs());
        if (market == null || !market.matches("[A-Z0-9][A-Z0-9_-]{0,31}")
                || rate.scale() < 0 || rate.scale() > 18)
            throw new IllegalArgumentException("funding market or rate precision is invalid");
        support.lockExecution();
        List<Map<String, Object>> instruments = jdbc.queryForList("SELECT instrument_id,policy_version "
                + "FROM trading_instruments WHERE market_symbol=? AND instrument_kind='PERPETUAL' "
                + "FOR UPDATE", market);
        if (instruments.size() != 1 || instruments.getFirst().get("policy_version") == null)
            throw new TradingOrderRejectedException("futures instrument policy is unavailable");
        String instrument = instruments.getFirst().get("instrument_id").toString();
        long version = ((Number) instruments.getFirst().get("policy_version")).longValue();
        FuturesRiskPolicy policy = policies.version(instrument, version);
        if (!"USDT".equals(policy.collateralAsset()))
            throw new TradingOrderRejectedException("non-USDT funding settlement is unavailable");
        if (rate.abs().compareTo(policy.maximumAbsoluteFundingRate()) > 0)
            throw new TradingOrderRejectedException("funding rate exceeds governed cap");
        Instant now = Instant.now();
        if (periodEnd.isAfter(now) || periodEnd.isBefore(policy.fundingScheduleAnchor())
                || periodEnd.getNano() != policy.fundingScheduleAnchor().getNano())
            throw new IllegalArgumentException("funding period is outside the schedule");
        long elapsed = Math.subtractExact(periodEnd.getEpochSecond(),
                policy.fundingScheduleAnchor().getEpochSecond());
        if (elapsed % policy.fundingInterval().getSeconds() != 0)
            throw new IllegalArgumentException("funding period is not aligned to policy schedule");
        String cycleId = "fc_" + TradingExecutionSupport.hash(instrument + ":" + periodEnd).substring(0, 48);
        List<Map<String, Object>> existing = jdbc.queryForList("SELECT cycle_id,funding_rate,mark_price_usdt,"
                + "ledger_journal_id FROM futures_funding_cycles WHERE instrument_id=? AND period_end=?",
                instrument, Timestamp.from(periodEnd));
        if (!existing.isEmpty()) {
            var row = existing.getFirst();
            if (((BigDecimal) row.get("funding_rate")).compareTo(rate) != 0)
                throw new IllegalArgumentException("funding cycle rate conflicts with prior settlement");
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM futures_funding_allocations "
                    + "WHERE cycle_id=?", Integer.class, cycleId);
            return new Result(cycleId, instrument, periodEnd, rate.toPlainString(),
                    ((BigDecimal) row.get("mark_price_usdt")).toPlainString(),
                    (Long) row.get("ledger_journal_id"), count);
        }
        Instant previousEnd = jdbc.query("SELECT MAX(period_end) FROM futures_funding_cycles "
                        + "WHERE instrument_id=?", result -> {
                    result.next();
                    java.sql.Timestamp value = result.getTimestamp(1);
                    return value == null ? null : value.toInstant();
                }, instrument);
        Instant expectedEnd = (previousEnd == null ? policy.fundingScheduleAnchor() : previousEnd)
                .plus(policy.fundingInterval());
        if (!periodEnd.equals(expectedEnd))
            throw new TradingOrderRejectedException("funding cycles must settle in order");
        var feed = references.getIfAvailable();
        var snapshot = feed == null ? null : feed.current(policy.execution().rules().baseAsset() + "USDT",
                policy.maximumPriceAge(), now).orElse(null);
        if (snapshot == null || !snapshot.source().equals(policy.markPriceSource())
                || Duration.between(periodEnd, snapshot.sourceAt()).abs().compareTo(policy.maximumPriceAge()) > 0)
            throw new TradingOrderRejectedException("funding mark price is unavailable at period end");
        jdbc.queryForObject("SELECT account_id FROM accounts WHERE account_id=? "
                + "AND account_category='EXCHANGE' AND status='ACTIVE' FOR UPDATE", String.class,
                policy.marginPoolAccountId());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT p.account_id,p.position_side,p.quantity,"
                + "p.isolated_collateral,p.position_version FROM futures_hedge_positions p "
                + "WHERE p.instrument_id=? AND p.quantity>0 ORDER BY p.account_id,p.position_side FOR UPDATE OF p",
                instrument);
        List<FuturesFundingAllocator.Position> positions = rows.stream().map(row ->
                new FuturesFundingAllocator.Position(row.get("account_id").toString(),
                        row.get("position_side").toString(), (BigDecimal) row.get("quantity"),
                        (BigDecimal) row.get("isolated_collateral"))).toList();
        List<FuturesFundingAllocator.Allocation> allocations = FuturesFundingAllocator.allocate(positions,
                snapshot.markPrice(), rate, policy.maintenanceMarginRate(),
                policy.execution().rules().quoteScale(), policy.settlementRounding());
        BigDecimal totalPaid = allocations.stream().filter(a -> a.fundingUsdt().signum() < 0)
                .map(a -> a.fundingUsdt().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long journalId = null;
        if (totalPaid.signum() > 0) {
            BigDecimal poolBalance = jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' "
                            + "THEN amount ELSE -amount END),0) FROM ledger_entries WHERE account_id=? "
                            + "AND asset_symbol='USDT'", BigDecimal.class, policy.marginPoolAccountId());
            if (poolBalance.compareTo(totalPaid) < 0)
                throw new TradingOrderRejectedException("margin pool cannot fund the cycle");
            List<LedgerEntryDraft> entries = new ArrayList<>();
            entries.add(entry(policy.marginPoolAccountId(), LedgerDirection.DEBIT, totalPaid, 1));
            for (var allocation : allocations) {
                if (allocation.fundingUsdt().signum() > 0)
                    entries.add(entry(allocation.accountId(), LedgerDirection.CREDIT,
                            allocation.fundingUsdt(), entries.size() + 1));
            }
            journalId = ledger.post(new LedgerPostingExecutionCommand(
                    new LedgerPostingCommand(new RequestId(cycleId),
                            new LedgerJournalDraft(LedgerBusinessReferenceType.SETTLEMENT, cycleId, entries), now),
                    cycleId, new LedgerPostingActor("ADMIN", actor.userId()))).ledgerJournalId();
        }
        jdbc.update("INSERT INTO futures_funding_cycles(cycle_id,instrument_id,policy_version,period_end,"
                        + "funding_rate,rate_source,mark_price_usdt,mark_source,mark_source_at,"
                        + "ledger_journal_id,settled_by) VALUES (?,?,?,?,?,'ADMIN_GOVERNED',?,?,?,?,?)",
                cycleId, instrument, version, Timestamp.from(periodEnd), rate, snapshot.markPrice(), snapshot.source(),
                Timestamp.from(snapshot.sourceAt()), journalId, actor.userId());
        for (var allocation : allocations) {
            Map<String, Object> before = rows.stream().filter(row ->
                    row.get("account_id").equals(allocation.accountId())
                            && row.get("position_side").equals(allocation.side())).findFirst().orElseThrow();
            long previousVersion = ((Number) before.get("position_version")).longValue();
            long nextVersion = Math.addExact(previousVersion, 1);
            if (jdbc.update("UPDATE futures_hedge_positions SET isolated_collateral=?,position_version=?,"
                            + "updated_at=CURRENT_TIMESTAMP WHERE account_id=? AND instrument_id=? "
                            + "AND position_side=? AND position_version=?", allocation.resultingCollateralUsdt(),
                    nextVersion, allocation.accountId(), instrument, allocation.side(), previousVersion) != 1)
                throw new IllegalStateException("funding position version changed");
            jdbc.update("INSERT INTO futures_funding_allocations(cycle_id,account_id,instrument_id,"
                            + "position_side,funding_usdt,previous_collateral,resulting_collateral,"
                            + "previous_version,next_version) VALUES (?,?,?,?,?,?,?,?,?)",
                    cycleId, allocation.accountId(), instrument, allocation.side(), allocation.fundingUsdt(),
                    before.get("isolated_collateral"), allocation.resultingCollateralUsdt(), previousVersion,
                    nextVersion);
        }
        String evidence = support.encode(Map.of("cycleId", cycleId, "instrumentId", instrument,
                "periodEnd", periodEnd.toString(), "rate", rate.toPlainString()));
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,"
                        + "request_id,outcome,after_state) VALUES ('ADMIN',?,'FUTURES_FUNDING_SETTLED',"
                        + "'TRADING_INSTRUMENT',?,?,'SUCCESS',?)",
                actor.userId(), instrument, cycleId, evidence);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) "
                        + "VALUES ('TRADING_INSTRUMENT',?,'FUTURES_FUNDING_SETTLED',?,'READY',?)",
                instrument, evidence, cycleId);
        return new Result(cycleId, instrument, periodEnd, rate.toPlainString(),
                snapshot.markPrice().toPlainString(), journalId, allocations.size());
    }

    private static LedgerEntryDraft entry(String account, LedgerDirection direction,
                                          BigDecimal amount, int sequence) {
        return new LedgerEntryDraft(new AccountId(account), new AssetSymbol("USDT"),
                direction, amount, sequence);
    }
}
