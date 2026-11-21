package com.lumix.trading.runtime;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 只保存逐倉政策證據；未完成結算與強平閉環前始終保持商品 HALTED。HUMAN_REVIEW_REQUIRED。 */
@Service
@Profile("infrastructure")
public class FuturesRiskPolicyAdministrationService {
    private final JdbcTemplate jdbc;
    private final TradingExecutionSupport support;
    private final SuperAdminAccessService access;

    public FuturesRiskPolicyAdministrationService(JdbcTemplate jdbc, TradingExecutionSupport support,
                                                  SuperAdminAccessService access) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.support = Objects.requireNonNull(support);
        this.access = Objects.requireNonNull(access);
    }

    /** 同版重送只接受相同 JSON；版本與稽核資料在同一資料庫交易內提交。 */
    @Transactional
    public FuturesRiskPolicy publish(AuthenticatedUser actor, FuturesRiskPolicy policy) {
        access.requireActiveSuperAdmin(actor);
        Objects.requireNonNull(policy, "complete futures policy is required");
        var rules = policy.execution().rules();
        if (!"BINANCE_USDSM_PREMIUM_INDEX".equals(policy.markPriceSource())
                || !policy.markPriceSource().equals(policy.indexPriceSource()))
            throw new IllegalArgumentException("named Binance USD-M mark and index source is required");
        support.lockExecution();
        String encoded = support.encode(policy);
        List<String> saved = jdbc.query("SELECT policy::text FROM futures_risk_policies "
                        + "WHERE instrument_id=? AND policy_version=?", (row, index) -> row.getString(1),
                rules.instrumentId(), rules.policyVersion());
        if (!saved.isEmpty()) {
            Boolean identical = jdbc.queryForObject("SELECT CAST(? AS jsonb)=CAST(? AS jsonb)", Boolean.class,
                    saved.getFirst(), encoded);
            if (!Boolean.TRUE.equals(identical)) throw new IllegalArgumentException("risk policy version conflicts");
            return policy;
        }
        List<Map<String, Object>> markets = jdbc.queryForList(
                "SELECT m.base_asset_symbol,m.quote_asset_symbol,b.precision_scale AS base_scale,q.precision_scale AS quote_scale "
                        + "FROM markets m JOIN assets b ON b.asset_symbol=m.base_asset_symbol "
                        + "JOIN assets q ON q.asset_symbol=m.quote_asset_symbol "
                        + "WHERE m.market_symbol=? AND m.status='ACTIVE' AND b.status='ACTIVE' AND q.status='ACTIVE'",
                rules.marketSymbol());
        if (markets.size() != 1) throw new IllegalArgumentException("active underlying market is required");
        var market = markets.getFirst();
        if (!rules.baseAsset().equals(market.get("base_asset_symbol"))
                || !rules.quoteAsset().equals(market.get("quote_asset_symbol"))
                || rules.quantityScale() > ((Number) market.get("base_scale")).intValue()
                || rules.quoteScale() != ((Number) market.get("quote_scale")).intValue())
            throw new IllegalArgumentException("futures market identity or precision mismatch");
        requireAsset(policy.collateralAsset());
        requireSystemAccount(policy.execution().feeAccountId(), "TRADING_FEE_REVENUE", "USDT");
        requireSystemAccount(policy.marginPoolAccountId(), "PERPETUAL_MARGIN_POOL", policy.collateralAsset());
        requireSystemAccount(policy.insuranceAccountId(), "PERPETUAL_INSURANCE", "USDT");
        requireSystemAccount(policy.conversionPoolAccountId(), "PERPETUAL_CONVERSION_POOL", "USDT");

        jdbc.update("INSERT INTO trading_instruments(instrument_id,instrument_kind,market_symbol,status) "
                        + "VALUES (?,'PERPETUAL',?,'HALTED') ON CONFLICT(instrument_id) DO NOTHING",
                rules.instrumentId(), rules.marketSymbol());
        var instrument = jdbc.queryForMap("SELECT instrument_kind,market_symbol,status FROM trading_instruments "
                + "WHERE instrument_id=? FOR UPDATE", rules.instrumentId());
        if (!"PERPETUAL".equals(instrument.get("instrument_kind"))
                || !rules.marketSymbol().equals(instrument.get("market_symbol")))
            throw new IllegalArgumentException("futures instrument identity is immutable");
        if (!"HALTED".equals(instrument.get("status")))
            throw new IllegalStateException("futures policy cannot be changed while instrument is active");

        Long latest = jdbc.queryForObject("SELECT COALESCE(MAX(policy_version),0) FROM trading_execution_policies "
                + "WHERE instrument_id=?", Long.class, rules.instrumentId());
        if (latest == null || rules.policyVersion() != latest + 1)
            throw new IllegalArgumentException("futures policy version must be sequential");
        Integer open = jdbc.queryForObject("SELECT COUNT(*) FROM orders o JOIN trading_order_executions e "
                + "ON e.order_id=o.order_id WHERE e.instrument_id=? AND o.status IN ('ACCEPTED','OPEN','PARTIALLY_FILLED')",
                Integer.class, rules.instrumentId());
        Integer positions = jdbc.queryForObject("SELECT COUNT(*) FROM futures_hedge_positions "
                + "WHERE instrument_id=? AND quantity>0", Integer.class, rules.instrumentId());
        if (open == null || open != 0 || positions == null || positions != 0)
            throw new IllegalStateException("cancel orders and close positions before changing policy");

        jdbc.update("INSERT INTO trading_execution_policies(instrument_id,policy_version,policy,created_by) "
                        + "VALUES (?,?,CAST(? AS jsonb),?)", rules.instrumentId(), rules.policyVersion(),
                support.encode(policy.execution()), actor.userId());
        jdbc.update("INSERT INTO futures_risk_policies(instrument_id,policy_version,policy,created_by) "
                        + "VALUES (?,?,CAST(? AS jsonb),?)", rules.instrumentId(), rules.policyVersion(),
                encoded, actor.userId());
        jdbc.update("UPDATE trading_instruments SET policy_version=?,updated_at=CURRENT_TIMESTAMP WHERE instrument_id=?",
                rules.policyVersion(), rules.instrumentId());
        String request = "futures-policy:" + rules.instrumentId() + ":" + rules.policyVersion();
        String evidence = support.encode(Map.of("instrumentId", rules.instrumentId(),
                "policyVersion", rules.policyVersion(), "status", "HALTED"));
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) "
                        + "VALUES ('ADMIN',?,'FUTURES_RISK_POLICY_PUBLISHED','TRADING_INSTRUMENT',? ,?,'SUCCESS',?)",
                actor.userId(), rules.instrumentId(), request, evidence);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) "
                        + "VALUES ('TRADING_INSTRUMENT',?,'FUTURES_RISK_POLICY_PUBLISHED',?,'READY',?)",
                rules.instrumentId(), evidence, request);
        return policy;
    }

    private void requireAsset(String asset) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assets WHERE asset_symbol=? AND status='ACTIVE'",
                Integer.class, asset);
        if (count == null || count != 1) throw new IllegalArgumentException("active collateral asset is required");
    }

    /** 政策只引用已存在的專用帳戶；此處不建立資金或假餘額。 */
    private void requireSystemAccount(String account, String purpose, String asset) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM accounts a JOIN account_assets aa "
                        + "ON aa.account_id=a.account_id WHERE a.account_id=? AND a.account_category='EXCHANGE' "
                        + "AND a.account_purpose=? AND a.status='ACTIVE' AND aa.asset_symbol=? "
                        + "AND aa.status='ACTIVE'", Integer.class, account, purpose, asset);
        if (count == null || count != 1) throw new IllegalArgumentException("governed system account asset is required");
    }
}
