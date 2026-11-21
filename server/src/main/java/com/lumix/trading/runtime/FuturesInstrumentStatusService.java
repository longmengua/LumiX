package com.lumix.trading.runtime;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 合約商品啟用須有完整 USDT 政策、新鮮行情及已入帳保險基金。 */
@Service
@Profile("infrastructure")
public class FuturesInstrumentStatusService {
    private final JdbcTemplate jdbc;
    private final FuturesRiskPolicyRepository policies;
    private final TradingExecutionSupport support;
    private final ObjectProvider<BinanceFuturesReferencePriceFeed> references;
    private final SuperAdminAccessService access;
    private final FuturesTradingExecutionService trading;

    public FuturesInstrumentStatusService(JdbcTemplate jdbc, FuturesRiskPolicyRepository policies,
            TradingExecutionSupport support, ObjectProvider<BinanceFuturesReferencePriceFeed> references,
            SuperAdminAccessService access, FuturesTradingExecutionService trading) {
        this.jdbc=jdbc;this.policies=policies;this.support=support;this.references=references;this.access=access;
        this.trading=trading;
    }

    public record Status(String marketSymbol, String status, long policyVersion) { }

    @Transactional
    public Status change(AuthenticatedUser actor, String market, String target) {
        access.requireActiveSuperAdmin(actor);
        if (market == null || !market.matches("[A-Z0-9][A-Z0-9_-]{0,31}")
                || !List.of("ACTIVE", "HALTED").contains(target))
            throw new IllegalArgumentException("invalid futures instrument status command");
        support.lockExecution();
        var rows = jdbc.queryForList("SELECT instrument_id,policy_version,status FROM trading_instruments "
                + "WHERE instrument_kind='PERPETUAL' AND market_symbol=? FOR UPDATE", market);
        if (rows.size()!=1) throw new TradingOrderRejectedException("futures instrument is unavailable");
        var row=rows.getFirst();
        String instrument=row.get("instrument_id").toString();
        long version=((Number)row.get("policy_version")).longValue();
        String current=row.get("status").toString();
        if (current.equals(target)) return new Status(market,target,version);
        if ("ACTIVE".equals(target)) {
            FuturesRiskPolicy policy=policies.version(instrument,version);
            if (!"USDT".equals(policy.collateralAsset()))
                throw new TradingOrderRejectedException("non-USDT collateral settlement is unavailable");
            var rules=policy.execution().rules();
            var feed=references.getIfAvailable();
            var snapshot=feed==null?null:feed.current(rules.baseAsset()+rules.quoteAsset(),
                    policy.maximumPriceAge(), Instant.now()).orElse(null);
            if (snapshot==null || !Objects.equals(snapshot.source(),policy.markPriceSource()))
                throw new TradingOrderRejectedException("fresh governed futures reference is required");
            BigDecimal insurance=jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' "
                    + "THEN amount ELSE -amount END),0) FROM ledger_entries "
                    + "WHERE account_id=? AND asset_symbol='USDT'",BigDecimal.class,policy.insuranceAccountId());
            if (insurance==null || insurance.signum()<=0)
                throw new TradingOrderRejectedException("insurance fund must be pre-funded");
        } else if ("ACTIVE".equals(current) || "REDUCE_ONLY".equals(current)) {
            Integer positions=jdbc.queryForObject("SELECT COUNT(*) FROM futures_hedge_positions "
                    + "WHERE instrument_id=? AND quantity>0",Integer.class,instrument);
            Integer orders=jdbc.queryForObject("SELECT COUNT(*) FROM orders o JOIN trading_order_executions x "
                    + "ON x.order_id=o.order_id WHERE x.instrument_id=? "
                    + "AND o.status IN ('ACCEPTED','OPEN','PARTIALLY_FILLED')",Integer.class,instrument);
            if (positions==null || positions>0 || orders==null || orders>0)
                throw new TradingOrderRejectedException("close positions and orders before halting instrument");
        }
        jdbc.update("UPDATE trading_instruments SET status=?,updated_at=CURRENT_TIMESTAMP WHERE instrument_id=?",
                target,instrument);
        String request="fis_"+TradingExecutionSupport.hash(java.util.UUID.randomUUID().toString()).substring(0,48);
        String evidence=support.encode(Map.of("instrumentId",instrument,"policyVersion",version,"status",target));
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) "
                + "VALUES ('ADMIN',?,'FUTURES_STATUS_CHANGED','TRADING_INSTRUMENT',?,?,'SUCCESS',?)",
                actor.userId(),instrument,request,evidence);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) "
                + "VALUES ('TRADING_INSTRUMENT',?,'FUTURES_STATUS_CHANGED',?,'READY',?)",
                instrument,evidence,request);
        return new Status(market,target,version);
    }

    /** 強平缺少真實對手盤或資金時即時停止新風險，並釋放既有開倉委託。 */
    @Transactional
    public void riskHalt(String market,String reason) {
        if (!List.of("NO_LIQUIDITY","INSURANCE_SHORTFALL","MARGIN_POOL_SHORTFALL").contains(reason))
            throw new IllegalArgumentException("unsupported futures risk halt reason");
        support.lockExecution();
        var rows=jdbc.queryForList("SELECT instrument_id,status FROM trading_instruments "
                + "WHERE instrument_kind='PERPETUAL' AND market_symbol=? FOR UPDATE",market);
        if (rows.size()!=1 || !"ACTIVE".equals(rows.getFirst().get("status"))) return;
        String instrument=rows.getFirst().get("instrument_id").toString();
        var open=jdbc.queryForList("SELECT o.order_id,o.user_id FROM orders o "
                + "JOIN trading_order_executions x ON x.order_id=o.order_id "
                + "WHERE x.instrument_id=? AND x.reduce_only=false "
                + "AND o.status IN ('ACCEPTED','OPEN','PARTIALLY_FILLED') "
                + "ORDER BY x.order_sequence",instrument);
        for(var order:open) trading.cancel(order.get("user_id").toString(),
                order.get("order_id").toString(),"risk-halt:"+order.get("order_id"));
        jdbc.update("UPDATE trading_instruments SET status='REDUCE_ONLY',updated_at=CURRENT_TIMESTAMP "
                + "WHERE instrument_id=?",instrument);
        String request="frh_"+TradingExecutionSupport.hash(java.util.UUID.randomUUID().toString()).substring(0,48);
        String evidence=support.encode(Map.of("instrumentId",instrument,"status","REDUCE_ONLY","reason",reason));
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) "
                + "VALUES ('SYSTEM','futures-liquidation','FUTURES_RISK_HALTED','TRADING_INSTRUMENT',?,?,'SUCCESS',?)",
                instrument,request,evidence);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) "
                + "VALUES ('TRADING_INSTRUMENT',?,'FUTURES_RISK_HALTED',?,'READY',?)",
                instrument,evidence,request);
    }
}
