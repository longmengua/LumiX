package com.lumix.trading.runtime;

import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.reservation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 真實現貨接單／撮合／結算／撤單。HUMAN_REVIEW_REQUIRED。
 * 全部 order、reservation、ledger、trade、audit/outbox 同交易成功或回滾；外部參考簿不是成交來源。
 */
@Service
public class SpotTradingExecutionService {
    private static final int MAXIMUM_MATCHES=100;
    private static final String SELECT="SELECT o.*,e.instrument_id,e.policy_version,e.order_sequence,e.reservation_id,e.filled_notional,e.fee_accrued,e.fee_charged "
            + "FROM orders o JOIN trading_order_executions e ON e.order_id=o.order_id ";
    private final JdbcTemplate jdbc;
    private final TradingPolicyRepository policies;
    private final TradingExecutionSupport support;
    private final ReservationHoldService holds;
    private final OrderReservationChangeService reservations;
    private final TradingOrderQueryService queries;

    public SpotTradingExecutionService(JdbcTemplate jdbc,TradingPolicyRepository policies,TradingExecutionSupport support,
            ReservationHoldService holds,OrderReservationChangeService reservations,TradingOrderQueryService queries) {
        this.jdbc=jdbc;this.policies=policies;this.support=support;this.holds=holds;this.reservations=reservations;this.queries=queries;
    }

    /** 接受有界 plain-decimal HTTP 命令；沒有 browser 指定的 owner/account 或隱含自動成交。 */
    public record Command(String marketSymbol,String side,String orderType,String timeInForce,String price,String quantity) { }

    /** 價格優先、持久化序號優先；FOK、競態或任何入帳錯誤會回滾整次命令。 */
    @Transactional
    public TradingOrderQueryService.OrderView place(String owner,Command command,String key) {
        Objects.requireNonNull(command,"order command is required");
        if(command.side()==null||command.orderType()==null||!Set.of("BUY","SELL").contains(command.side())||!Set.of("LIMIT","MARKET").contains(command.orderType()))throw new IllegalArgumentException("unsupported order type or side");
        if(command.marketSymbol()==null||!command.marketSymbol().matches("[A-Z0-9][A-Z0-9_-]{0,31}"))throw new IllegalArgumentException("market symbol is invalid");
        String tif=command.timeInForce()==null?("MARKET".equals(command.orderType())?"IOC":"GTC"):command.timeInForce();
        if(!Set.of("GTC","IOC","FOK").contains(tif)||("MARKET".equals(command.orderType())&&"GTC".equals(tif)))throw new IllegalArgumentException("unsupported time in force");
        BigDecimal quantity=TradingDecimal.positive(command.quantity());
        BigDecimal inputPrice="LIMIT".equals(command.orderType())?TradingDecimal.positive(command.price()):null;
        if("MARKET".equals(command.orderType())&&command.price()!=null)throw new IllegalArgumentException("market order cannot specify a price");
        String fingerprint=TradingExecutionSupport.hash(support.encode(Arrays.asList(command.marketSymbol(),command.side(),command.orderType(),tif,inputPrice,quantity)));
        String request="order-"+UUID.randomUUID();
        support.lockExecution();
        String replay=support.claim("ORDER_CREATE",owner,key,fingerprint,request);
        if(replay!=null)return queries.detail(owner,"spot",replay);
        TradingExecutionPolicy policy=policies.active("spot",command.marketSymbol());
        TradingInstrumentRules rule=policy.rules();
        if(rule.kind()!=TradingInstrumentRules.Kind.SPOT)throw new IllegalStateException("spot instrument policy required");
        support.requireFeeAccount(policy.feeAccountId(),rule.quoteAsset());
        String account=support.activeAccount(owner,"spot");
        BigDecimal price=inputPrice==null?protectedPrice(policy,command.side()):inputPrice;
        TradingLimitOrderValues values=TradingLimitOrderValues.validate(rule,price.toPlainString(),quantity.toPlainString());
        List<Map<String,Object>> makers=makers(rule.instrumentId(),command.side(),price);
        List<String> accounts=new ArrayList<>();accounts.add(account);
        for(var maker:makers)accounts.add(maker.get("account_id").toString());
        support.lockAccounts(accounts);
        for(String a:new TreeSet<>(accounts)){support.requireAsset(a,rule.baseAsset());support.requireAsset(a,rule.quoteAsset());}
        Long open=jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE account_id=? AND status IN ('OPEN','PARTIALLY_FILLED','ACCEPTED')",Long.class,account);
        if(open==null||open>=policy.maximumOpenOrders())throw new TradingOrderRejectedException("open order limit reached");
        String order="ord_"+UUID.randomUUID(),reservation="hold_"+UUID.randomUUID();
        jdbc.update("INSERT INTO orders(order_id,user_id,account_id,market_symbol,side,order_type,time_in_force,status,price,quantity,remaining_quantity,request_id) "
                + "VALUES (?,?,?,?,?,?,?,'ACCEPTED',?,?,?,?)",order,owner,account,rule.marketSymbol(),command.side(),command.orderType(),tif,price,quantity,quantity,request);
        BigDecimal amount="BUY".equals(command.side())?values.notional().add(ceiling(values.notional().multiply(maximumRate(rule)),rule.quoteScale())):quantity;
        TradingDecimal.ledgerAmount(amount);
        // 已持有帳戶與 projection 鎖，這裡檢查真實可用額；缺少入帳證據時不能假定有可用資金。
        List<BigDecimal> available=jdbc.query("SELECT available_amount FROM balance_projections WHERE account_id=? AND asset_symbol=?",(rs,n)->rs.getBigDecimal(1),account,holdAsset(command.side(),rule));
        if(available.size()!=1||available.getFirst().compareTo(amount)<0)throw new com.lumix.api.error.ApiException(com.lumix.api.error.ApiErrorCode.INSUFFICIENT_USER_BALANCE);
        holds.hold(new ReservationHoldCommand(reservation,account,holdAsset(command.side(),rule),order,amount,request,"trading-hold:"+order,owner,"ORDER","ORDER_HOLD"));
        jdbc.update("INSERT INTO trading_order_executions(order_id,instrument_id,policy_version,reservation_id,request_hash) VALUES (?,?,?,?,?)",order,rule.instrumentId(),rule.policyVersion(),reservation,fingerprint);
        for(var candidate:makers) {
            Map<String,Object> incoming=order(order);
            if(decimal(incoming,"remaining_quantity").signum()==0)break;
            Map<String,Object> maker=order(candidate.get("order_id").toString());
            if(owner.equals(maker.get("user_id")))throw new TradingOrderRejectedException("self trading is not allowed");
            BigDecimal fill=decimal(incoming,"remaining_quantity").min(decimal(maker,"remaining_quantity"));
            settle(incoming,maker,fill,request);
        }
        Map<String,Object> result=order(order);
        BigDecimal remaining=decimal(result,"remaining_quantity");
        if(remaining.signum()>0&&"FOK".equals(tif))throw new TradingOrderRejectedException("fill-or-kill order has insufficient executable liquidity");
        if(remaining.signum()>0&&makers.size()>=MAXIMUM_MATCHES&&"GTC".equals(tif)
                &&!makers(rule.instrumentId(),command.side(),price).isEmpty())throw new TradingOrderRejectedException("execution budget exceeded");
        if(remaining.signum()>0&&"IOC".equals(tif)) {
            releaseAll(result,rule,request,"ioc");
            jdbc.update("UPDATE orders SET status='CANCELED',updated_at=CURRENT_TIMESTAMP WHERE order_id=?",order);
        } else if(remaining.signum()>0)jdbc.update("UPDATE orders SET status=?,updated_at=CURRENT_TIMESTAMP WHERE order_id=?",decimal(result,"filled_quantity").signum()==0?"OPEN":"PARTIALLY_FILLED",order);
        support.evidence(owner,"TRADING_ORDER_ACCEPTED",order,request,Map.of("instrument",rule.instrumentId(),"policyVersion",rule.policyVersion(),"timeInForce",tif));
        support.complete("ORDER_CREATE",owner,key,order);
        return queries.detail(owner,"spot",order);
    }

    /** 已暫停的市場仍可 owner 安全撤單；僅釋放該單尚未使用的保留，不退回已成交資金。 */
    @Transactional
    public TradingOrderQueryService.OrderView cancel(String owner,String orderId,String key) {
        support.lockExecution();
        String request="cancel-"+UUID.randomUUID();
        String fingerprint=TradingExecutionSupport.hash(orderId);
        String replay=support.claim("ORDER_CANCEL",owner,key,fingerprint,request);
        if(replay!=null)return queries.detail(owner,"spot",replay);
        queries.detail(owner,"spot",orderId);
        Map<String,Object> order=order(orderId);
        TradingExecutionPolicy policy=policies.version(order.get("instrument_id").toString(),((Number)order.get("policy_version")).longValue());
        if(policy.rules().kind()!=TradingInstrumentRules.Kind.SPOT)throw new NoSuchElementException("order not found");
        String state=order.get("status").toString();
        if(!Set.of("OPEN","ACCEPTED","PARTIALLY_FILLED","CANCELED").contains(state))throw new TradingOrderRejectedException("order cannot be canceled");
        if(!"CANCELED".equals(state)) {
            releaseAll(order,policy.rules(),request,"cancel");
            jdbc.update("UPDATE orders SET status='CANCELED',updated_at=CURRENT_TIMESTAMP WHERE order_id=?",orderId);
            support.evidence(owner,"TRADING_ORDER_CANCELED",orderId,request,Map.of("remaining",decimal(order,"remaining_quantity").toPlainString()));
        }
        support.complete("ORDER_CANCEL",owner,key,orderId);
        return queries.detail(owner,"spot",orderId);
    }

    private List<Map<String,Object>> makers(String instrument,String side,BigDecimal price) {
        String comparison="BUY".equals(side)?"<=":">=",priority="BUY".equals(side)?"ASC":"DESC";
        return jdbc.queryForList(SELECT+"JOIN accounts a ON a.account_id=o.account_id JOIN users u ON u.user_id=o.user_id "
                +"WHERE a.status='ACTIVE' AND u.status='ACTIVE' AND e.instrument_id=? AND o.side=? AND o.status IN ('OPEN','PARTIALLY_FILLED') AND o.price "+comparison+" ? "
                +"ORDER BY o.price "+priority+",e.order_sequence ASC LIMIT "+MAXIMUM_MATCHES,instrument,"BUY".equals(side)?"SELL":"BUY",price);
    }

    private BigDecimal protectedPrice(TradingExecutionPolicy policy,String side) {
        boolean buy="BUY".equals(side);
        List<BigDecimal> prices=jdbc.query("SELECT o.price FROM orders o JOIN trading_order_executions e ON e.order_id=o.order_id "
                +"JOIN accounts a ON a.account_id=o.account_id JOIN users u ON u.user_id=o.user_id "
                +"WHERE a.status='ACTIVE' AND u.status='ACTIVE' AND e.instrument_id=? AND o.side=? AND o.status IN ('OPEN','PARTIALLY_FILLED') ORDER BY o.price "+(buy?"ASC":"DESC")+",e.order_sequence LIMIT 1",(r,n)->r.getBigDecimal(1),policy.rules().instrumentId(),buy?"SELL":"BUY");
        if(prices.isEmpty())throw new TradingOrderRejectedException("market order requires real executable liquidity");
        BigDecimal bound=prices.getFirst().multiply(buy?BigDecimal.ONE.add(policy.maximumSlippage()):BigDecimal.ONE.subtract(policy.maximumSlippage()));
        return bound.divide(policy.rules().priceTick(),0,buy?RoundingMode.FLOOR:RoundingMode.CEILING).multiply(policy.rules().priceTick());
    }

    private void settle(Map<String,Object> taker,Map<String,Object> maker,BigDecimal quantity,String request) {
        TradingExecutionPolicy makerPolicy=policies.version(maker.get("instrument_id").toString(),((Number)maker.get("policy_version")).longValue());
        TradingExecutionPolicy takerPolicy=policies.version(taker.get("instrument_id").toString(),((Number)taker.get("policy_version")).longValue());
        TradingInstrumentRules rule=takerPolicy.rules();
        BigDecimal price=decimal(maker,"price"),notional=price.multiply(quantity).setScale(rule.quoteScale(),RoundingMode.UNNECESSARY);
        TradingDecimal.ledgerAmount(notional);
        BigDecimal makerFee=fee(maker,notional,makerPolicy.rules().makerFeeRate(),makerPolicy.rules());
        BigDecimal takerFee=fee(taker,notional,takerPolicy.rules().takerFeeRate(),takerPolicy.rules());
        boolean takerBuy="BUY".equals(taker.get("side"));
        Map<String,Object> buyer=takerBuy?taker:maker,seller=takerBuy?maker:taker;
        TradingExecutionPolicy buyerPolicy=takerBuy?takerPolicy:makerPolicy,sellerPolicy=takerBuy?makerPolicy:takerPolicy;
        BigDecimal buyerFee=takerBuy?takerFee:makerFee,sellerFee=takerBuy?makerFee:takerFee;
        String trade="trade_"+UUID.randomUUID();
        change(buyer,rule,OrderReservationChange.Action.CAPTURE,notional.add(buyerFee),request,trade+":buy");
        change(seller,rule,OrderReservationChange.Action.CAPTURE,quantity,request,trade+":sell");
        List<LedgerEntryDraft> entries=new ArrayList<>();
        support.entry(entries,buyer.get("account_id").toString(),rule.quoteAsset(),LedgerDirection.DEBIT,notional.add(buyerFee));
        support.entry(entries,seller.get("account_id").toString(),rule.quoteAsset(),LedgerDirection.CREDIT,notional.subtract(sellerFee));
        support.entry(entries,buyerPolicy.feeAccountId(),rule.quoteAsset(),LedgerDirection.CREDIT,buyerFee);
        support.entry(entries,sellerPolicy.feeAccountId(),rule.quoteAsset(),LedgerDirection.CREDIT,sellerFee);
        support.entry(entries,seller.get("account_id").toString(),rule.baseAsset(),LedgerDirection.DEBIT,quantity);
        support.entry(entries,buyer.get("account_id").toString(),rule.baseAsset(),LedgerDirection.CREDIT,quantity);
        long journal=support.post(trade,request,entries);
        jdbc.update("INSERT INTO trades(trade_id,market_symbol,maker_order_id,taker_order_id,maker_account_id,taker_account_id,price,quantity,quote_quantity,maker_fee_amount,taker_fee_amount,fee_asset_symbol) "
                +"VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",trade,rule.marketSymbol(),maker.get("order_id"),taker.get("order_id"),maker.get("account_id"),taker.get("account_id"),price,quantity,notional,makerFee,takerFee,rule.quoteAsset());
        jdbc.update("INSERT INTO trading_trade_executions(trade_id,instrument_id,maker_policy_version,taker_policy_version,ledger_journal_id) VALUES (?,?,?,?,?)",trade,rule.instrumentId(),makerPolicy.rules().policyVersion(),takerPolicy.rules().policyVersion(),journal);
        updateFilled(maker,quantity,notional,makerFee,makerPolicy.rules().makerFeeRate());
        updateFilled(taker,quantity,notional,takerFee,takerPolicy.rules().takerFeeRate());
        releaseImprovement(order(buyer.get("order_id").toString()),buyerPolicy.rules(),request,trade);
        support.evidence(taker.get("user_id").toString(),"TRADING_TRADE_SETTLED",taker.get("order_id").toString(),request,Map.of("tradeId",trade,"journalId",journal,"quantity",quantity.toPlainString(),"price",price.toPlainString()));
    }

    private BigDecimal fee(Map<String,Object> order,BigDecimal notional,BigDecimal rate,TradingInstrumentRules rules) {
        return decimal(order,"fee_accrued").add(notional.multiply(rate)).setScale(rules.quoteScale(),rules.feeRounding()).subtract(decimal(order,"fee_charged"));
    }
    private void updateFilled(Map<String,Object> order,BigDecimal quantity,BigDecimal notional,BigDecimal fee,BigDecimal rate) {
        BigDecimal remaining=decimal(order,"remaining_quantity").subtract(quantity);
        jdbc.update("UPDATE orders SET filled_quantity=filled_quantity+?,remaining_quantity=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE order_id=?",quantity,remaining,remaining.signum()==0?"FILLED":"PARTIALLY_FILLED",order.get("order_id"));
        jdbc.update("UPDATE trading_order_executions SET filled_notional=filled_notional+?,fee_accrued=fee_accrued+?,fee_charged=fee_charged+? WHERE order_id=?",notional,notional.multiply(rate),fee,order.get("order_id"));
    }
    private void releaseImprovement(Map<String,Object> buyer,TradingInstrumentRules rules,String request,String trade) {
        BigDecimal remaining=decimal(buyer,"remaining_quantity"),needed=BigDecimal.ZERO;
        if(remaining.signum()>0) {
            BigDecimal principal=remaining.multiply(decimal(buyer,"price")).setScale(rules.quoteScale(),RoundingMode.UNNECESSARY);
            BigDecimal futureFee=ceiling(decimal(buyer,"fee_accrued").add(principal.multiply(maximumRate(rules))),rules.quoteScale()).subtract(decimal(buyer,"fee_charged"));
            needed=principal.add(futureFee);
        }
        BigDecimal reserved=jdbc.queryForObject("SELECT remaining_amount FROM reservations WHERE reservation_id=?",BigDecimal.class,buyer.get("reservation_id"));
        BigDecimal excess=Objects.requireNonNull(reserved).subtract(needed);
        if(excess.signum()<0)throw new IllegalStateException("remaining order is under-reserved");
        if(excess.signum()>0)change(buyer,rules,OrderReservationChange.Action.RELEASE,excess,request,trade+":improvement");
    }
    private void releaseAll(Map<String,Object> order,TradingInstrumentRules rule,String request,String reason) {
        BigDecimal amount=jdbc.queryForObject("SELECT remaining_amount FROM reservations WHERE reservation_id=?",BigDecimal.class,order.get("reservation_id"));
        if(amount!=null&&amount.signum()>0)change(order,rule,OrderReservationChange.Action.RELEASE,amount,request,reason+":"+order.get("order_id"));
    }
    private void change(Map<String,Object> order,TradingInstrumentRules rule,OrderReservationChange.Action action,BigDecimal amount,String request,String key) {
        reservations.change(new OrderReservationChange(action,order.get("user_id").toString(),order.get("account_id").toString(),order.get("order_id").toString(),order.get("reservation_id").toString(),holdAsset(order.get("side").toString(),rule),amount,request,key));
    }
    private Map<String,Object> order(String id) {return jdbc.queryForMap(SELECT+"WHERE o.order_id=? FOR UPDATE OF o,e",id);}
    private static BigDecimal decimal(Map<String,Object> row,String field){return (BigDecimal)row.get(field);}
    private static BigDecimal maximumRate(TradingInstrumentRules rules){return rules.makerFeeRate().max(rules.takerFeeRate());}
    private static BigDecimal ceiling(BigDecimal amount,int scale){return amount.setScale(scale,RoundingMode.CEILING);}
    private static String holdAsset(String side,TradingInstrumentRules rules){return "BUY".equals(side)?rules.quoteAsset():rules.baseAsset();}
}
