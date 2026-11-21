package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import com.lumix.reservation.OrderReservationChange;
import com.lumix.reservation.OrderReservationChangeService;
import com.lumix.reservation.ReservationHoldCommand;
import com.lumix.reservation.ReservationHoldService;
import com.lumix.trading.core.futures.position.FuturesHedgePositionTransition;
import com.lumix.trading.core.futures.position.FuturesPositionSide;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** USDT 逐倉客戶互撮與原子結算；所有缺少資金或行情的路徑拒絕成交。 */
@Service
public class FuturesTradingExecutionService {
    private static final int MAXIMUM_MATCHES = 100;
    private static final String SELECT = "SELECT o.*,x.instrument_id,x.policy_version,x.order_sequence,"
            + "x.reservation_id,x.filled_notional,x.fee_accrued,x.fee_charged,x.position_side,"
            + "x.reduce_only,x.leverage FROM orders o JOIN trading_order_executions x ON x.order_id=o.order_id ";
    private final JdbcTemplate jdbc;
    private final FuturesRiskPolicyRepository policies;
    private final TradingExecutionSupport support;
    private final ReservationHoldService holds;
    private final OrderReservationChangeService reservations;
    private final TradingOrderQueryService queries;
    private final ObjectProvider<BinanceFuturesReferencePriceFeed> references;
    private final FuturesHedgePositionStore positions;

    public FuturesTradingExecutionService(JdbcTemplate jdbc, FuturesRiskPolicyRepository policies,
            TradingExecutionSupport support, ReservationHoldService holds,
            OrderReservationChangeService reservations, TradingOrderQueryService queries,
            ObjectProvider<BinanceFuturesReferencePriceFeed> references, ObjectMapper json) {
        this.jdbc = jdbc;
        this.policies = policies;
        this.support = support;
        this.holds = holds;
        this.reservations = reservations;
        this.queries = queries;
        this.references = references;
        this.positions = new FuturesHedgePositionStore(jdbc, json);
    }

    public record Command(String marketSymbol, String side, String positionSide, boolean reduceOnly,
                          String orderType, String timeInForce, String price, String quantity,
                          String leverage) { }

    @Transactional
    public TradingOrderQueryService.OrderView place(String owner, Command command, String key) {
        Objects.requireNonNull(command, "futures order is required");
        if (command.marketSymbol() == null || !command.marketSymbol().matches("[A-Z0-9][A-Z0-9_-]{0,31}")
                || !Set.of("BUY", "SELL").contains(command.side())
                || !Set.of("LONG", "SHORT").contains(command.positionSide())
                || !Set.of("LIMIT", "MARKET").contains(command.orderType()))
            throw new IllegalArgumentException("unsupported futures order");
        boolean opening = opening(command.side(), command.positionSide());
        if (opening == command.reduceOnly())
            throw new IllegalArgumentException("position direction and reduce-only disagree");
        String tif = command.timeInForce() == null
                ? ("MARKET".equals(command.orderType()) ? "IOC" : "GTC") : command.timeInForce();
        if (!Set.of("GTC", "IOC", "FOK").contains(tif)
                || ("MARKET".equals(command.orderType()) && "GTC".equals(tif)))
            throw new IllegalArgumentException("unsupported time in force");
        BigDecimal quantity = TradingDecimal.positive(command.quantity());
        BigDecimal requestedPrice = "LIMIT".equals(command.orderType())
                ? TradingDecimal.positive(command.price()) : null;
        if ("MARKET".equals(command.orderType()) && command.price() != null)
            throw new IllegalArgumentException("market order cannot specify a price");
        BigDecimal leverage = TradingDecimal.positive(command.leverage());
        String fingerprint = TradingExecutionSupport.hash(support.encode(Arrays.asList(command.marketSymbol(),
                command.side(), command.positionSide(), command.reduceOnly(), command.orderType(), tif,
                requestedPrice, quantity, leverage)));
        String request = "fut-" + UUID.randomUUID();
        support.lockExecution();
        String replay = support.claim("FUTURES_ORDER_CREATE", owner, key, fingerprint, request);
        if (replay != null) return queries.detail(owner, "futures", replay);
        FuturesRiskPolicy policy = policies.active(command.marketSymbol());
        if (!"USDT".equals(policy.collateralAsset()))
            throw new TradingOrderRejectedException("non-USDT collateral runtime is unavailable");
        var rules = policy.execution().rules();
        String account = support.activeAccount(owner, "futures");
        BigDecimal price = requestedPrice == null ? protectedPrice(policy, command.side()) : requestedPrice;
        TradingLimitOrderValues.validate(rules, price.toPlainString(), quantity.toPlainString());
        List<Map<String, Object>> makers = makers(rules.instrumentId(), command.side(), price);
        TreeSet<String> accounts = new TreeSet<>();
        accounts.add(account);
        accounts.add(policy.marginPoolAccountId());
        accounts.add(policy.execution().feeAccountId());
        for (var maker : makers) accounts.add(maker.get("account_id").toString());
        lockAccounts(accounts);
        support.requireAsset(account, "USDT");
        for (var maker : makers) support.requireAsset(maker.get("account_id").toString(), "USDT");
        BigDecimal reserve = BigDecimal.ZERO;
        if (opening) {
            var feed = references.getIfAvailable();
            var snapshot = feed == null ? null : feed.current(rules.baseAsset() + rules.quoteAsset(),
                    policy.maximumPriceAge(), Instant.now()).orElse(null);
            if (snapshot == null) throw new TradingOrderRejectedException("fresh futures mark price is required");
            BigDecimal available = available(account, "USDT");
            BigDecimal existing = jdbc.queryForObject("SELECT COALESCE(SUM(quantity * ?),0) FROM "
                    + "futures_hedge_positions WHERE account_id=? AND instrument_id=? AND position_side=?",
                    BigDecimal.class, snapshot.markPrice(), account, rules.instrumentId(), command.positionSide());
            var decision = FuturesOrderRiskGate.admit(policy, snapshot, null, quantity, price,
                    leverage, existing, available, BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
            reserve = decision.initialMarginUsdt().add(decision.maximumFeeUsdt());
        } else {
            BigDecimal positionQuantity = jdbc.queryForObject("SELECT COALESCE(SUM(quantity),0) "
                    + "FROM futures_hedge_positions WHERE account_id=? AND instrument_id=? AND position_side=?",
                    BigDecimal.class, account, rules.instrumentId(), command.positionSide());
            BigDecimal pending = jdbc.queryForObject("SELECT COALESCE(SUM(o.remaining_quantity),0) FROM orders o "
                    + "JOIN trading_order_executions x ON x.order_id=o.order_id WHERE o.account_id=? "
                    + "AND x.instrument_id=? AND x.position_side=? AND x.reduce_only=true "
                    + "AND o.status IN ('ACCEPTED','OPEN','PARTIALLY_FILLED')", BigDecimal.class,
                    account, rules.instrumentId(), command.positionSide());
            if (positionQuantity.subtract(pending).compareTo(quantity) < 0)
                throw new TradingOrderRejectedException("reduce-only quantity exceeds available position");
        }
        Integer openOrders = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE account_id=? "
                + "AND status IN ('ACCEPTED','OPEN','PARTIALLY_FILLED')", Integer.class, account);
        if (openOrders == null || openOrders >= policy.execution().maximumOpenOrders())
            throw new TradingOrderRejectedException("futures open order limit reached");
        String orderId = "ord_" + UUID.randomUUID();
        String reservationId = opening ? "hold_" + UUID.randomUUID() : null;
        jdbc.update("INSERT INTO orders(order_id,user_id,account_id,market_symbol,side,order_type,"
                        + "time_in_force,status,price,quantity,remaining_quantity,request_id) "
                        + "VALUES (?,?,?,?,?,?,?,'ACCEPTED',?,?,?,?)", orderId, owner, account,
                rules.marketSymbol(), command.side(), command.orderType(), tif, price, quantity, quantity, request);
        if (opening) holds.hold(new ReservationHoldCommand(reservationId, account, "USDT", orderId,
                reserve, request, "futures-hold:" + orderId, owner, "ORDER", "ORDER_HOLD"));
        jdbc.update("INSERT INTO trading_order_executions(order_id,instrument_id,policy_version,"
                        + "reservation_id,request_hash,position_side,reduce_only,leverage) "
                        + "VALUES (?,?,?,?,?,?,?,?)", orderId, rules.instrumentId(), rules.policyVersion(),
                reservationId, fingerprint, command.positionSide(), command.reduceOnly(), leverage);
        for (var candidate : makers) {
            var incoming = order(orderId);
            if (decimal(incoming, "remaining_quantity").signum() == 0) break;
            var maker = order(candidate.get("order_id").toString());
            if (owner.equals(maker.get("user_id"))) throw new TradingOrderRejectedException("self trading is not allowed");
            BigDecimal fill = decimal(incoming, "remaining_quantity").min(decimal(maker, "remaining_quantity"));
            settle(incoming, maker, fill, request);
        }
        var result = order(orderId);
        BigDecimal remaining = decimal(result, "remaining_quantity");
        if (remaining.signum() > 0 && "FOK".equals(tif))
            throw new TradingOrderRejectedException("fill-or-kill has insufficient futures liquidity");
        if (remaining.signum() > 0 && makers.size() >= MAXIMUM_MATCHES && "GTC".equals(tif)
                && !makers(rules.instrumentId(), command.side(), price).isEmpty())
            throw new TradingOrderRejectedException("execution budget exceeded");
        if (remaining.signum() > 0 && "IOC".equals(tif)) {
            releaseAll(result, request);
            jdbc.update("UPDATE orders SET status='CANCELED',updated_at=CURRENT_TIMESTAMP WHERE order_id=?", orderId);
        } else if (remaining.signum() > 0) {
            jdbc.update("UPDATE orders SET status=?,updated_at=CURRENT_TIMESTAMP WHERE order_id=?",
                    decimal(result, "filled_quantity").signum() == 0 ? "OPEN" : "PARTIALLY_FILLED", orderId);
        } else releaseAll(result, request);
        support.complete("FUTURES_ORDER_CREATE", owner, key, orderId);
        support.evidence(owner, "FUTURES_ORDER_ACCEPTED", orderId, request,
                Map.of("instrumentId", rules.instrumentId(), "positionSide", command.positionSide()));
        return queries.detail(owner, "futures", orderId);
    }

    @Transactional
    public TradingOrderQueryService.OrderView cancel(String owner, String orderId, String key) {
        String request = "fut-cancel-" + UUID.randomUUID();
        support.lockExecution();
        String replay = support.claim("FUTURES_ORDER_CANCEL", owner, key,
                TradingExecutionSupport.hash(orderId), request);
        if (replay != null) return queries.detail(owner, "futures", replay);
        queries.detail(owner, "futures", orderId);
        var order = order(orderId);
        if (!Set.of("OPEN", "ACCEPTED", "PARTIALLY_FILLED", "CANCELED").contains(order.get("status")))
            throw new TradingOrderRejectedException("futures order cannot be canceled");
        if (!"CANCELED".equals(order.get("status"))) {
            releaseAll(order, request);
            jdbc.update("UPDATE orders SET status='CANCELED',updated_at=CURRENT_TIMESTAMP WHERE order_id=?", orderId);
            support.evidence(owner, "FUTURES_ORDER_CANCELED", orderId, request, Map.of("orderId", orderId));
        }
        support.complete("FUTURES_ORDER_CANCEL", owner, key, orderId);
        return queries.detail(owner, "futures", orderId);
    }

    private void settle(Map<String, Object> taker, Map<String, Object> maker, BigDecimal quantity, String request) {
        FuturesRiskPolicy takerPolicy = policies.version(taker.get("instrument_id").toString(),
                ((Number) taker.get("policy_version")).longValue());
        FuturesRiskPolicy makerPolicy = policies.version(maker.get("instrument_id").toString(),
                ((Number) maker.get("policy_version")).longValue());
        if (!takerPolicy.marginPoolAccountId().equals(makerPolicy.marginPoolAccountId()))
            throw new TradingOrderRejectedException("margin pool changed across open orders");
        var rule = takerPolicy.execution().rules();
        BigDecimal price = decimal(maker, "price");
        BigDecimal notional = price.multiply(quantity).setScale(rule.quoteScale(), RoundingMode.UNNECESSARY);
        BigDecimal makerFee = fee(maker, notional, makerPolicy.execution().rules().makerFeeRate(), rule);
        BigDecimal takerFee = fee(taker, notional, takerPolicy.execution().rules().takerFeeRate(), rule);
        Leg makerLeg = leg(maker, makerPolicy, quantity, price, makerFee);
        Leg takerLeg = leg(taker, takerPolicy, quantity, price, takerFee);
        checkOpeningFillRisk(maker, makerPolicy, quantity, price);
        checkOpeningFillRisk(taker, takerPolicy, quantity, price);
        String trade = "trade_" + UUID.randomUUID();
        capture(maker, makerLeg, request, trade);
        capture(taker, takerLeg, request, trade);
        List<LedgerEntryDraft> entries = new ArrayList<>();
        append(entries, maker, makerPolicy, makerLeg);
        append(entries, taker, takerPolicy, takerLeg);
        BigDecimal poolOutgoing = makerLeg.poolDebit().add(takerLeg.poolDebit())
                .subtract(makerLeg.poolCredit()).subtract(takerLeg.poolCredit());
        if (poolOutgoing.signum() > 0 && available(takerPolicy.marginPoolAccountId(), "USDT")
                .compareTo(poolOutgoing) < 0)
            throw new TradingOrderRejectedException("margin pool lacks settled USDT backing");
        long journal = support.post(trade, request, entries);
        jdbc.update("INSERT INTO trades(trade_id,market_symbol,maker_order_id,taker_order_id,"
                        + "maker_account_id,taker_account_id,price,quantity,quote_quantity,"
                        + "maker_fee_amount,taker_fee_amount,fee_asset_symbol) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", trade, rule.marketSymbol(), maker.get("order_id"),
                taker.get("order_id"), maker.get("account_id"), taker.get("account_id"), price, quantity,
                notional, makerFee, takerFee, "USDT");
        jdbc.update("INSERT INTO trading_trade_executions(trade_id,instrument_id,maker_policy_version,"
                        + "taker_policy_version,ledger_journal_id) VALUES (?,?,?,?,?)", trade,
                rule.instrumentId(), makerPolicy.execution().rules().policyVersion(),
                takerPolicy.execution().rules().policyVersion(), journal);
        positions.recordFill(trade, maker.get("order_id").toString(), makerLeg.resultingCollateral());
        positions.recordFill(trade, taker.get("order_id").toString(), takerLeg.resultingCollateral());
        filled(maker, quantity, notional, makerFee, makerPolicy.execution().rules().makerFeeRate());
        filled(taker, quantity, notional, takerFee, takerPolicy.execution().rules().takerFeeRate());
        support.evidence(taker.get("user_id").toString(), "FUTURES_TRADE_SETTLED",
                taker.get("order_id").toString(), request,
                Map.of("tradeId", trade, "journalId", journal, "quantity", quantity.toPlainString()));
    }

    private record Leg(boolean opening, BigDecimal margin, BigDecimal fee, BigDecimal payout,
                       BigDecimal resultingCollateral) {
        BigDecimal poolDebit() { return opening ? BigDecimal.ZERO : payout.add(fee); }
        BigDecimal poolCredit() { return opening ? margin : BigDecimal.ZERO; }
    }

    private void checkOpeningFillRisk(Map<String, Object> order, FuturesRiskPolicy policy,
                                      BigDecimal quantity, BigDecimal price) {
        if (!opening(order.get("side").toString(), order.get("position_side").toString())) return;
        var rules = policy.execution().rules();
        var feed = references.getIfAvailable();
        var snapshot = feed == null ? null : feed.current(rules.baseAsset() + rules.quoteAsset(),
                policy.maximumPriceAge(), Instant.now()).orElse(null);
        if (snapshot == null) throw new TradingOrderRejectedException("fresh futures mark price is required at fill");
        BigDecimal reserved = jdbc.queryForObject("SELECT remaining_amount FROM reservations "
                + "WHERE reservation_id=?", BigDecimal.class, order.get("reservation_id"));
        BigDecimal existing = jdbc.queryForObject("SELECT COALESCE(SUM(quantity * ?),0) "
                + "FROM futures_hedge_positions WHERE account_id=? AND instrument_id=? AND position_side=?",
                BigDecimal.class, snapshot.markPrice(), order.get("account_id"), rules.instrumentId(),
                order.get("position_side"));
        FuturesOrderRiskGate.admit(policy, snapshot, null, quantity, price,
                decimal(order, "leverage"), existing, reserved, BigDecimal.ZERO, BigDecimal.ZERO,
                Instant.now());
    }

    private Leg leg(Map<String, Object> order, FuturesRiskPolicy policy, BigDecimal quantity,
                    BigDecimal price, BigDecimal fee) {
        var rules = policy.execution().rules();
        boolean opening = opening(order.get("side").toString(), order.get("position_side").toString());
        String account = order.get("account_id").toString();
        if (opening) {
            BigDecimal margin = price.multiply(quantity).divide(decimal(order, "leverage"),
                    rules.quoteScale(), RoundingMode.CEILING);
            BigDecimal previous = currentCollateral(account, rules.instrumentId(),
                    order.get("position_side").toString());
            return new Leg(true, margin, fee, BigDecimal.ZERO, previous.add(margin));
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT quantity,entry_notional_usdt,"
                        + "isolated_collateral FROM futures_hedge_positions WHERE account_id=? AND instrument_id=? "
                        + "AND position_side=? FOR UPDATE", account, rules.instrumentId(), order.get("position_side"));
        if (rows.size() != 1) throw new TradingOrderRejectedException("reduce-only position is unavailable");
        var before = rows.getFirst();
        BigDecimal oldQty = (BigDecimal) before.get("quantity");
        BigDecimal collateral = (BigDecimal) before.get("isolated_collateral");
        var side = FuturesPositionSide.valueOf(order.get("position_side").toString());
        var result = FuturesHedgePositionTransition.apply(
                new FuturesHedgePositionTransition.Position(side, oldQty,
                        (BigDecimal) before.get("entry_notional_usdt")), side,
                FuturesHedgePositionTransition.OrderSide.valueOf(order.get("side").toString()),
                quantity, price, true, rules.quoteScale(), policy.settlementRounding());
        BigDecimal released = oldQty.compareTo(quantity) == 0 ? collateral
                : collateral.multiply(quantity).divide(oldQty, rules.quoteScale(), RoundingMode.DOWN);
        BigDecimal payout = released.add(result.realizedPnl()).subtract(fee);
        if (payout.signum() < 0)
            throw new TradingOrderRejectedException("bankrupt close requires liquidation settlement");
        return new Leg(false, released, fee, payout, collateral.subtract(released));
    }

    private void append(List<LedgerEntryDraft> entries, Map<String, Object> order,
                        FuturesRiskPolicy policy, Leg leg) {
        String account = order.get("account_id").toString();
        if (leg.opening()) {
            support.entry(entries, account, "USDT", LedgerDirection.DEBIT, leg.margin().add(leg.fee()));
            support.entry(entries, policy.marginPoolAccountId(), "USDT", LedgerDirection.CREDIT, leg.margin());
        } else {
            support.entry(entries, policy.marginPoolAccountId(), "USDT", LedgerDirection.DEBIT,
                    leg.payout().add(leg.fee()));
            support.entry(entries, account, "USDT", LedgerDirection.CREDIT, leg.payout());
        }
        support.entry(entries, policy.execution().feeAccountId(), "USDT", LedgerDirection.CREDIT, leg.fee());
    }

    private void capture(Map<String, Object> order, Leg leg, String request, String trade) {
        if (!leg.opening()) return;
        BigDecimal amount = leg.margin().add(leg.fee());
        String reservation = Objects.toString(order.get("reservation_id"), null);
        if (reservation == null) throw new IllegalStateException("opening margin reservation is missing");
        reservations.change(new OrderReservationChange(OrderReservationChange.Action.CAPTURE,
                order.get("user_id").toString(), order.get("account_id").toString(),
                order.get("order_id").toString(), reservation, "USDT", amount, request,
                trade + ":margin:" + order.get("order_id")));
    }

    private void filled(Map<String, Object> order, BigDecimal quantity, BigDecimal notional,
                        BigDecimal fee, BigDecimal rate) {
        BigDecimal remaining = decimal(order, "remaining_quantity").subtract(quantity);
        jdbc.update("UPDATE orders SET filled_quantity=filled_quantity+?,remaining_quantity=?,status=?,"
                        + "updated_at=CURRENT_TIMESTAMP WHERE order_id=?", quantity, remaining,
                remaining.signum() == 0 ? "FILLED" : "PARTIALLY_FILLED", order.get("order_id"));
        jdbc.update("UPDATE trading_order_executions SET filled_notional=filled_notional+?,"
                        + "fee_accrued=fee_accrued+?,fee_charged=fee_charged+? WHERE order_id=?",
                notional, notional.multiply(rate), fee, order.get("order_id"));
    }

    private BigDecimal fee(Map<String, Object> order, BigDecimal notional, BigDecimal rate,
                           TradingInstrumentRules rule) {
        return decimal(order, "fee_accrued").add(notional.multiply(rate))
                .setScale(rule.quoteScale(), rule.feeRounding()).subtract(decimal(order, "fee_charged"));
    }

    private void releaseAll(Map<String, Object> order, String request) {
        if (order.get("reservation_id") == null) return;
        BigDecimal amount = jdbc.queryForObject("SELECT remaining_amount FROM reservations WHERE reservation_id=?",
                BigDecimal.class, order.get("reservation_id"));
        if (amount != null && amount.signum() > 0)
            reservations.change(new OrderReservationChange(OrderReservationChange.Action.RELEASE,
                    order.get("user_id").toString(), order.get("account_id").toString(),
                    order.get("order_id").toString(), order.get("reservation_id").toString(),
                    "USDT", amount, request, "futures-release:" + order.get("order_id")));
    }

    private BigDecimal currentCollateral(String account, String instrument, String side) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(isolated_collateral),0) FROM futures_hedge_positions "
                + "WHERE account_id=? AND instrument_id=? AND position_side=?", BigDecimal.class,
                account, instrument, side);
    }

    private BigDecimal available(String account, String asset) {
        String category = jdbc.queryForObject("SELECT account_category FROM accounts WHERE account_id=?",
                String.class, account);
        if ("EXCHANGE".equals(category)) {
            return jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' THEN amount "
                            + "ELSE -amount END),0) FROM ledger_entries WHERE account_id=? AND asset_symbol=?",
                    BigDecimal.class, account, asset);
        }
        List<BigDecimal> rows = jdbc.query("SELECT available_amount FROM balance_projections "
                + "WHERE account_id=? AND asset_symbol=?", (row, index) -> row.getBigDecimal(1), account, asset);
        return rows.isEmpty() ? BigDecimal.ZERO : rows.getFirst();
    }

    private void lockAccounts(TreeSet<String> accounts) {
        for (String account : accounts) {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT a.account_id,a.account_category,"
                    + "a.status,u.status AS user_status FROM accounts a JOIN users u ON u.user_id=a.user_id "
                    + "WHERE a.account_id=? FOR UPDATE OF a,u", account);
            if (rows.size() != 1 || !"ACTIVE".equals(rows.getFirst().get("status"))
                    || ("USER".equals(rows.getFirst().get("account_category"))
                    && !"ACTIVE".equals(rows.getFirst().get("user_status"))))
                throw new TradingOrderRejectedException("futures settlement account is unavailable");
            jdbc.query("SELECT asset_symbol FROM balance_projections WHERE account_id=? "
                    + "ORDER BY asset_symbol FOR UPDATE", (row, index) -> row.getString(1), account);
        }
    }

    private List<Map<String, Object>> makers(String instrument, String side, BigDecimal price) {
        String comparison = "BUY".equals(side) ? "<=" : ">=";
        String priority = "BUY".equals(side) ? "ASC" : "DESC";
        return jdbc.queryForList(SELECT + "JOIN accounts a ON a.account_id=o.account_id "
                + "JOIN users u ON u.user_id=o.user_id WHERE a.status='ACTIVE' AND u.status='ACTIVE' "
                + "AND x.instrument_id=? AND o.side=? AND o.status IN ('OPEN','PARTIALLY_FILLED') "
                + "AND o.price " + comparison + " ? ORDER BY o.price " + priority
                + ",x.order_sequence ASC LIMIT " + MAXIMUM_MATCHES,
                instrument, "BUY".equals(side) ? "SELL" : "BUY", price);
    }

    private BigDecimal protectedPrice(FuturesRiskPolicy policy, String side) {
        String priority = "BUY".equals(side) ? "ASC" : "DESC";
        List<BigDecimal> best = jdbc.query("SELECT o.price FROM orders o JOIN trading_order_executions x "
                        + "ON x.order_id=o.order_id JOIN accounts a ON a.account_id=o.account_id "
                        + "JOIN users u ON u.user_id=o.user_id WHERE x.instrument_id=? "
                        + "AND o.side=? AND o.status IN ('OPEN','PARTIALLY_FILLED') "
                        + "AND a.status='ACTIVE' AND u.status='ACTIVE' ORDER BY o.price " + priority
                        + ",x.order_sequence LIMIT 1", (row, index) -> row.getBigDecimal(1),
                policy.execution().rules().instrumentId(), "BUY".equals(side) ? "SELL" : "BUY");
        if (best.isEmpty()) throw new TradingOrderRejectedException("market order needs real futures liquidity");
        boolean buy = "BUY".equals(side);
        BigDecimal bound = best.getFirst().multiply(buy
                ? BigDecimal.ONE.add(policy.execution().maximumSlippage())
                : BigDecimal.ONE.subtract(policy.execution().maximumSlippage()));
        BigDecimal tick = policy.execution().rules().priceTick();
        return bound.divide(tick, 0, buy ? RoundingMode.FLOOR : RoundingMode.CEILING).multiply(tick);
    }

    private Map<String, Object> order(String id) {
        return jdbc.queryForMap(SELECT + "WHERE o.order_id=? FOR UPDATE OF o,x", id);
    }

    private static BigDecimal decimal(Map<String, Object> row, String field) {
        return (BigDecimal) row.get(field);
    }

    private static boolean opening(String orderSide, String positionSide) {
        return ("LONG".equals(positionSide) && "BUY".equals(orderSide))
                || ("SHORT".equals(positionSide) && "SELL".equals(orderSide));
    }
}
