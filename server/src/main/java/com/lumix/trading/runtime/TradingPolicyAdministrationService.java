package com.lumix.trading.runtime;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 只發佈明確完整的現貨政策；不以任何本機測試數字作為 runtime 預設。HUMAN_REVIEW_REQUIRED。 */
@Service
@org.springframework.context.annotation.Profile("infrastructure")
public class TradingPolicyAdministrationService {
    private final com.lumix.admin.superadmin.SuperAdminAccessService access;
    private final JdbcTemplate jdbc;
    private final TradingExecutionSupport support;
    private final TradingPolicyRepository policies;
    public TradingPolicyAdministrationService(JdbcTemplate jdbc,TradingExecutionSupport support,TradingPolicyRepository policies,com.lumix.admin.superadmin.SuperAdminAccessService access){this.jdbc=jdbc;this.support=support;this.policies=policies;this.access=access;}

    /** 呼叫端必須通過最高管理員驗證；相同版本只接受完全相同政策，不能覆寫既有成交依據。 */
    @Transactional
    public TradingExecutionPolicy publishSpot(com.lumix.user.auth.domain.AuthenticatedUser actor,TradingExecutionPolicy policy) {
        access.requireActiveSuperAdmin(actor);
        if(policy==null||policy.rules().kind()!=TradingInstrumentRules.Kind.SPOT)throw new IllegalArgumentException("only a complete spot policy can be published");
        var r=policy.rules();support.lockExecution();
        List<Map<String,Object>> markets=jdbc.queryForList("SELECT m.*,b.precision_scale AS base_scale,q.precision_scale AS quote_scale FROM markets m JOIN assets b ON b.asset_symbol=m.base_asset_symbol JOIN assets q ON q.asset_symbol=m.quote_asset_symbol WHERE m.market_symbol=? AND m.status='ACTIVE' AND b.status='ACTIVE' AND q.status='ACTIVE'",r.marketSymbol());
        if(markets.size()!=1)throw new IllegalArgumentException("active underlying market required");
        var market=markets.getFirst();
        if(!r.baseAsset().equals(market.get("base_asset_symbol"))||!r.quoteAsset().equals(market.get("quote_asset_symbol"))
                ||r.quantityScale()>((Number)market.get("base_scale")).intValue()||r.quoteScale()!=((Number)market.get("quote_scale")).intValue())throw new IllegalArgumentException("policy asset identity or precision mismatch");
        jdbc.update("INSERT INTO trading_instruments(instrument_id,instrument_kind,market_symbol,status) VALUES (?,'SPOT',?,'HALTED') ON CONFLICT(instrument_id) DO NOTHING",r.instrumentId(),r.marketSymbol());
        var instrument=jdbc.queryForMap("SELECT * FROM trading_instruments WHERE instrument_id=? FOR UPDATE",r.instrumentId());
        if(!"SPOT".equals(instrument.get("instrument_kind"))||!r.marketSymbol().equals(instrument.get("market_symbol")))throw new IllegalArgumentException("instrument identity is immutable");
        List<String> previous=jdbc.query("SELECT policy::text FROM trading_execution_policies WHERE instrument_id=? AND policy_version=?",(rs,n)->rs.getString(1),r.instrumentId(),r.policyVersion());
        if(!previous.isEmpty()) {
            var saved=policies.version(r.instrumentId(),r.policyVersion());
            // JSON 結構比較保留 BigDecimal 的金融語意，不因 JSONB key 排序而誤判重送。
            Boolean identical=jdbc.queryForObject("SELECT CAST(? AS jsonb)=CAST(? AS jsonb)",Boolean.class,previous.getFirst(),support.encode(policy));
            if(!Boolean.TRUE.equals(identical))throw new IllegalArgumentException("policy version already has different contents");
            return saved;
        }
        Long maximum=jdbc.queryForObject("SELECT COALESCE(MAX(policy_version),0) FROM trading_execution_policies WHERE instrument_id=?",Long.class,r.instrumentId());
        if(r.policyVersion()!=maximum+1)throw new IllegalArgumentException("policy version must be sequential");
        Integer open=jdbc.queryForObject("SELECT COUNT(*) FROM orders o JOIN trading_order_executions e ON e.order_id=o.order_id WHERE e.instrument_id=? AND o.status IN ('ACCEPTED','OPEN','PARTIALLY_FILLED')",Integer.class,r.instrumentId());
        if(open!=0)throw new IllegalStateException("cancel open orders before changing execution policy");
        ensureFeeAccount(policy.feeAccountId(),r.quoteAsset());
        jdbc.update("INSERT INTO trading_execution_policies(instrument_id,policy_version,policy,created_by) VALUES (?,?,CAST(? AS jsonb),?)",r.instrumentId(),r.policyVersion(),support.encode(policy),actor.userId());
        jdbc.update("UPDATE trading_instruments SET policy_version=?,status='ACTIVE',updated_at=CURRENT_TIMESTAMP WHERE instrument_id=?",r.policyVersion(),r.instrumentId());
        String request="policy:"+TradingExecutionSupport.hash(r.instrumentId()+":"+r.policyVersion()).substring(0,48);
        String evidence=support.encode(Map.of("policyVersion",r.policyVersion(),"policy",policy));
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('ADMIN',?,'TRADING_POLICY_PUBLISHED','TRADING_INSTRUMENT',?,?,'SUCCESS',?)",actor.userId(),r.instrumentId(),request,evidence);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('TRADING_INSTRUMENT',?,'TRADING_POLICY_PUBLISHED',?,'READY',?)",r.instrumentId(),evidence,request);
        return policies.active("spot",r.marketSymbol());
    }

    /** 系統帳戶僅建立空的收費容器；沒有入帳、預填餘額或對手方資金。 */
    private void ensureFeeAccount(String account,String asset) {
        if(!"system:trading-fees:spot".equals(account)) {
            support.requireFeeAccount(account,asset);return;
        }
        jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES ('system:trading-fees','system-trading-fees@internal.lumix.invalid','交易費用系統帳戶','SUSPENDED') ON CONFLICT(user_id) DO NOTHING");
        jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) VALUES (?,'system:trading-fees','SPOT','ACTIVE','EXCHANGE','TRADING_FEE_REVENUE') ON CONFLICT(account_id) DO NOTHING",account);
        support.requireAsset(account,asset);support.requireFeeAccount(account,asset);
    }
}
