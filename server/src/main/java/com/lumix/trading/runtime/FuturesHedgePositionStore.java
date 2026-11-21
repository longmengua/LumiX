package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.trading.core.futures.position.FuturesHedgePositionTransition;
import com.lumix.trading.core.futures.position.FuturesPositionSide;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 實際成交後的雙向持倉投影；只供同交易的帳本結算呼叫，不暴露客戶 API。 */
final class FuturesHedgePositionStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    FuturesHedgePositionStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.json = Objects.requireNonNull(json);
    }

    record Applied(String eventId, long version, BigDecimal quantity, BigDecimal entryNotionalUsdt,
                   BigDecimal isolatedCollateral, BigDecimal realizedPnlUsdt) { }

    /** 成交、journal 與指定委託須互相對應；相同成交只記錄一次持倉事件。 */
    Applied recordFill(String tradeId, String orderId, BigDecimal resultingCollateral) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
            throw new IllegalStateException("position mutation requires the settlement transaction");
        Objects.requireNonNull(resultingCollateral, "settled collateral is required");
        TradingDecimal.ledgerAmount(resultingCollateral);
        if (resultingCollateral.signum() < 0) throw new IllegalArgumentException("isolated collateral cannot be negative");
        List<Map<String, Object>> fills = jdbc.queryForList(
                "SELECT t.trade_id,t.price,t.quantity,t.maker_order_id,t.maker_fee_amount,t.taker_fee_amount,"
                        + "o.account_id,o.side,x.instrument_id,x.position_side,"
                        + "x.reduce_only,x.liquidation,x.policy_version,e.ledger_journal_id,a.account_type,a.account_category "
                        + "FROM trades t JOIN trading_trade_executions e ON e.trade_id=t.trade_id "
                        + "JOIN orders o ON o.order_id IN (t.maker_order_id,t.taker_order_id) "
                        + "JOIN trading_order_executions x ON x.order_id=o.order_id "
                        + "JOIN accounts a ON a.account_id=o.account_id "
                        + "JOIN ledger_journals j ON j.ledger_journal_id=e.ledger_journal_id "
                        + "WHERE t.trade_id=? AND o.order_id=? AND o.account_id IN (t.maker_account_id,t.taker_account_id) "
                        + "AND x.instrument_id=e.instrument_id AND x.policy_version IN (e.maker_policy_version,e.taker_policy_version) "
                        + "AND j.business_reference_type='TRADE' AND j.business_reference_id=t.trade_id",
                tradeId, orderId);
        if (fills.size() != 1) throw new IllegalArgumentException("settled futures fill is required");
        var fill = fills.getFirst();
        if (!"FUTURES".equals(fill.get("account_type")) || !"USER".equals(fill.get("account_category"))
                || fill.get("position_side") == null)
            throw new IllegalArgumentException("owned futures order position is required");
        String account = fill.get("account_id").toString();
        String instrument = fill.get("instrument_id").toString();
        String side = fill.get("position_side").toString();
        long policyVersion = ((Number) fill.get("policy_version")).longValue();
        FuturesRiskPolicy policy = policy(instrument, policyVersion);
        if (!"USDT".equals(policy.collateralAsset()))
            throw new TradingOrderRejectedException("non-USDT collateral settlement is unavailable");
        var rules = policy.execution().rules();
        FuturesPositionSide positionSide = FuturesPositionSide.valueOf(side);

        String eventId = "fp_" + TradingExecutionSupport.hash(tradeId + ":" + account + ":" + side).substring(0, 48);
        List<Map<String, Object>> prior = jdbc.queryForList(
                "SELECT next_version,resulting_quantity,resulting_entry_notional_usdt,"
                        + "resulting_isolated_collateral,realized_pnl_usdt "
                        + "FROM futures_hedge_position_events WHERE event_id=?", eventId);
        if (!prior.isEmpty()) {
            var event = prior.getFirst();
            return new Applied(eventId, ((Number) event.get("next_version")).longValue(),
                    (BigDecimal) event.get("resulting_quantity"),
                    (BigDecimal) event.get("resulting_entry_notional_usdt"),
                    (BigDecimal) event.get("resulting_isolated_collateral"),
                    (BigDecimal) event.get("realized_pnl_usdt"));
        }
        jdbc.update("INSERT INTO futures_hedge_positions(account_id,instrument_id,position_side,quantity,"
                        + "entry_notional_usdt,collateral_asset_symbol,isolated_collateral,position_version) "
                        + "VALUES (?,?,?,0,0,?,0,0) ON CONFLICT(account_id,instrument_id,position_side) DO NOTHING",
                account, instrument, side, policy.collateralAsset());
        Map<String, Object> before = jdbc.queryForMap("SELECT quantity,entry_notional_usdt,"
                        + "collateral_asset_symbol,isolated_collateral,position_version FROM futures_hedge_positions "
                        + "WHERE account_id=? AND instrument_id=? AND position_side=? FOR UPDATE",
                account, instrument, side);
        if (!policy.collateralAsset().equals(before.get("collateral_asset_symbol")))
            throw new IllegalStateException("position collateral cannot change while exposure exists");
        BigDecimal previousQuantity = (BigDecimal) before.get("quantity");
        BigDecimal previousCollateral = (BigDecimal) before.get("isolated_collateral");
        boolean reduce = Boolean.TRUE.equals(fill.get("reduce_only"));
        var transition = FuturesHedgePositionTransition.apply(
                new FuturesHedgePositionTransition.Position(positionSide, previousQuantity,
                        (BigDecimal) before.get("entry_notional_usdt")), positionSide,
                FuturesHedgePositionTransition.OrderSide.valueOf(fill.get("side").toString()),
                (BigDecimal) fill.get("quantity"), (BigDecimal) fill.get("price"), reduce,
                rules.quoteScale(), policy.settlementRounding());
        boolean opening = transition.position().quantity().compareTo(previousQuantity) > 0;
        if ((opening && (reduce || resultingCollateral.compareTo(previousCollateral) <= 0))
                || (!opening && (!reduce || resultingCollateral.compareTo(previousCollateral) >= 0))
                || (transition.position().quantity().signum() == 0 && resultingCollateral.signum() != 0)
                || (transition.position().quantity().signum() > 0 && resultingCollateral.signum() == 0))
            throw new IllegalArgumentException("settled collateral does not match position direction");
        if (!opening) {
            BigDecimal release = transition.position().quantity().signum() == 0 ? previousCollateral
                    : previousCollateral.multiply((BigDecimal) fill.get("quantity"))
                    .divide(previousQuantity, rules.quoteScale(), RoundingMode.DOWN);
            if (resultingCollateral.compareTo(previousCollateral.subtract(release)) != 0)
                throw new IllegalArgumentException("closing collateral must be released pro rata");
        }
        BigDecimal collateralChange = resultingCollateral.subtract(previousCollateral).abs();
        BigDecimal fee = (BigDecimal) fill.get(orderId.equals(fill.get("maker_order_id"))
                ? "maker_fee_amount" : "taker_fee_amount");
        long journalId = ((Number) fill.get("ledger_journal_id")).longValue();
        BigDecimal userNetDebit = netDebit(journalId, account, "USDT");
        BigDecimal poolCredits = gross(journalId, policy.marginPoolAccountId(), "USDT", "CREDIT");
        BigDecimal poolDebits = gross(journalId, policy.marginPoolAccountId(), "USDT", "DEBIT");
        if (opening) {
            if (userNetDebit.compareTo(collateralChange.add(fee)) != 0
                    || poolCredits.compareTo(collateralChange) < 0)
                throw new IllegalStateException("opening margin is not backed by the trade ledger");
        } else {
            BigDecimal rawPayout = collateralChange.add(transition.realizedPnl()).subtract(fee);
            boolean liquidation = Boolean.TRUE.equals(fill.get("liquidation"));
            if (rawPayout.signum() < 0 && !liquidation)
                throw new TradingOrderRejectedException("bankrupt close requires liquidation settlement");
            BigDecimal payout = rawPayout.max(BigDecimal.ZERO);
            BigDecimal deficit = rawPayout.negate().max(BigDecimal.ZERO);
            BigDecimal insuranceDebits = gross(journalId, policy.insuranceAccountId(), "USDT", "DEBIT");
            BigDecimal expectedPoolDebit = payout.add(fee).subtract(deficit).max(BigDecimal.ZERO);
            BigDecimal expectedPoolCredit = deficit.subtract(payout.add(fee)).max(BigDecimal.ZERO);
            if (userNetDebit.compareTo(payout.negate()) != 0
                    || poolDebits.compareTo(expectedPoolDebit) < 0
                    || poolCredits.compareTo(expectedPoolCredit) < 0
                    || insuranceDebits.compareTo(deficit) < 0)
                throw new IllegalStateException("closing PnL is not backed by the trade ledger");
        }
        long previousVersion = ((Number) before.get("position_version")).longValue();
        long nextVersion = Math.addExact(previousVersion, 1);
        String eventType = opening ? (previousQuantity.signum() == 0 ? "OPEN" : "INCREASE")
                : Boolean.TRUE.equals(fill.get("liquidation")) ? "LIQUIDATE"
                : (transition.position().quantity().signum() == 0 ? "CLOSE" : "DECREASE");
        if (jdbc.update("UPDATE futures_hedge_positions SET quantity=?,entry_notional_usdt=?,"
                        + "isolated_collateral=?,position_version=?,updated_at=CURRENT_TIMESTAMP "
                        + "WHERE account_id=? AND instrument_id=? AND position_side=? AND position_version=?",
                transition.position().quantity(), transition.position().entryNotionalUsdt() == null
                        ? BigDecimal.ZERO : transition.position().entryNotionalUsdt(), resultingCollateral,
                nextVersion, account, instrument, side, previousVersion) != 1)
            throw new IllegalStateException("position version changed during settlement");
        jdbc.update("INSERT INTO futures_hedge_position_events(event_id,account_id,instrument_id,position_side,"
                        + "event_type,trade_id,previous_version,next_version,fill_quantity,fill_price_usdt,"
                        + "resulting_quantity,resulting_entry_notional_usdt,resulting_isolated_collateral,"
                        + "realized_pnl_usdt,ledger_journal_id,policy_version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", eventId, account, instrument, side,
                eventType, tradeId, previousVersion, nextVersion, fill.get("quantity"), fill.get("price"),
                transition.position().quantity(), transition.position().entryNotionalUsdt() == null
                        ? BigDecimal.ZERO : transition.position().entryNotionalUsdt(), resultingCollateral,
                transition.realizedPnl(), fill.get("ledger_journal_id"), policyVersion);
        return new Applied(eventId, nextVersion, transition.position().quantity(),
                transition.position().entryNotionalUsdt(), resultingCollateral, transition.realizedPnl());
    }

    private BigDecimal netDebit(long journalId, String account, String asset) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='DEBIT' THEN amount ELSE -amount END),0) "
                + "FROM ledger_entries WHERE ledger_journal_id=? AND account_id=? AND asset_symbol=?",
                BigDecimal.class, journalId, account, asset);
    }

    private BigDecimal gross(long journalId, String account, String asset, String direction) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM ledger_entries "
                + "WHERE ledger_journal_id=? AND account_id=? AND asset_symbol=? AND direction=?",
                BigDecimal.class, journalId, account, asset, direction);
    }

    private FuturesRiskPolicy policy(String instrument, long version) {
        List<String> rows = jdbc.query("SELECT p.policy::text FROM futures_risk_policies p "
                        + "JOIN trading_instruments i ON i.instrument_id=p.instrument_id "
                        + "WHERE p.instrument_id=? AND p.policy_version=? AND i.instrument_kind='PERPETUAL'",
                (row, index) -> row.getString(1), instrument, version);
        if (rows.size() != 1) throw new IllegalStateException("futures risk policy evidence is missing");
        try {
            FuturesRiskPolicy value = json.readValue(rows.getFirst(), FuturesRiskPolicy.class);
            if (!value.execution().rules().instrumentId().equals(instrument)
                    || value.execution().rules().policyVersion() != version)
                throw new IllegalStateException("futures risk policy identity mismatch");
            return value;
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new IllegalStateException("futures risk policy evidence is invalid", invalid);
        }
    }
}
