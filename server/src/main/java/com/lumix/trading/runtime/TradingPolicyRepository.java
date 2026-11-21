package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 從 primary 讀取不可變政策；缺少商品、完整版本或可用資產時拒絕新單。 */
@Repository
public class TradingPolicyRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public TradingPolicyRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=Objects.requireNonNull(jdbc); this.json=Objects.requireNonNull(json); }

    /** 商品與其政策用單一查詢固定，避免現貨與永續混簿或套錯版本。 */
    public TradingExecutionPolicy active(String kind, String market) {
        String type=kind(kind);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT p.policy::text AS policy,i.instrument_id,i.instrument_kind,i.market_symbol,p.policy_version,m.base_asset_symbol,m.quote_asset_symbol,b.precision_scale AS base_scale,q.precision_scale AS quote_scale FROM trading_instruments i "
                + "JOIN trading_execution_policies p ON p.instrument_id=i.instrument_id AND p.policy_version=i.policy_version "
                + "JOIN markets m ON m.market_symbol=i.market_symbol JOIN assets b ON b.asset_symbol=m.base_asset_symbol "
                + "JOIN assets q ON q.asset_symbol=m.quote_asset_symbol WHERE i.instrument_kind=? AND i.market_symbol=? "
                + "AND i.status='ACTIVE' AND m.status='ACTIVE' AND b.status='ACTIVE' AND q.status='ACTIVE'",type,market);
        if(rows.size()!=1)throw new NoSuchElementException("trading instrument is unavailable");
        return verified(rows.getFirst());
    }

    /** 已接單版本不能因治理更新而改變；撤單與成交仍能讀回原始政策。 */
    public TradingExecutionPolicy version(String instrument,long version) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT p.policy::text AS policy,i.instrument_id,i.instrument_kind,i.market_symbol,p.policy_version,m.base_asset_symbol,m.quote_asset_symbol,b.precision_scale AS base_scale,q.precision_scale AS quote_scale "
                + "FROM trading_execution_policies p JOIN trading_instruments i ON i.instrument_id=p.instrument_id "
                + "JOIN markets m ON m.market_symbol=i.market_symbol JOIN assets b ON b.asset_symbol=m.base_asset_symbol "
                + "JOIN assets q ON q.asset_symbol=m.quote_asset_symbol WHERE p.instrument_id=? AND p.policy_version=?",instrument,version);
        if(rows.size()!=1)throw new IllegalStateException("execution policy evidence missing");
        return verified(rows.getFirst());
    }

    /** JSON 政策必須符合資料庫商品身分與資產精度，防止錯版政策將其他商品資金混入成交。 */
    private TradingExecutionPolicy verified(Map<String,Object> row) {
        TradingExecutionPolicy policy=decode(row.get("policy").toString());
        TradingInstrumentRules rule=policy.rules();
        if(!rule.instrumentId().equals(row.get("instrument_id")) || !rule.kind().name().equals(row.get("instrument_kind"))
                || !rule.marketSymbol().equals(row.get("market_symbol")) || rule.policyVersion()!=((Number)row.get("policy_version")).longValue()
                || !rule.baseAsset().equals(row.get("base_asset_symbol")) || !rule.quoteAsset().equals(row.get("quote_asset_symbol"))
                || rule.quantityScale()>((Number)row.get("base_scale")).intValue() || rule.quoteScale()!=((Number)row.get("quote_scale")).intValue())
            throw new IllegalStateException("execution policy does not match instrument identity or precision");
        return policy;
    }

    private TradingExecutionPolicy decode(String value) {
        try { return json.readValue(value,TradingExecutionPolicy.class); }
        catch(Exception exception){throw new IllegalStateException("execution policy is invalid",exception);}
    }

    /** API kind 是明確白名單，不能從 browser 指定任意帳戶類型。 */
    public static String kind(String kind) {
        if("spot".equals(kind))return "SPOT";
        if("futures".equals(kind))return "PERPETUAL";
        throw new IllegalArgumentException("unsupported trading kind");
    }
}
