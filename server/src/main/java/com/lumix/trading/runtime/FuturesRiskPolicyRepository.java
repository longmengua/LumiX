package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 從 primary 讀取同版合約執行與風險政策；缺少任一完整政策即拒絕新單。 */
@Repository
public class FuturesRiskPolicyRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public FuturesRiskPolicyRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.json = Objects.requireNonNull(json);
    }

    public FuturesRiskPolicy active(String market) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.policy::text AS risk_policy,p.policy::text AS execution_policy,"
                        + "i.instrument_id,i.policy_version,i.market_symbol "
                        + "FROM trading_instruments i JOIN futures_risk_policies r "
                        + "ON r.instrument_id=i.instrument_id AND r.policy_version=i.policy_version "
                        + "JOIN trading_execution_policies p ON p.instrument_id=i.instrument_id "
                        + "AND p.policy_version=i.policy_version JOIN markets m ON m.market_symbol=i.market_symbol "
                        + "WHERE i.instrument_kind='PERPETUAL' AND i.market_symbol=? AND i.status='ACTIVE' "
                        + "AND m.status='ACTIVE'", market);
        if (rows.size() != 1) throw new NoSuchElementException("active futures risk policy is unavailable");
        return verified(rows.getFirst());
    }

    public FuturesRiskPolicy version(String instrument, long version) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.policy::text AS risk_policy,p.policy::text AS execution_policy,"
                        + "i.instrument_id,r.policy_version,i.market_symbol "
                        + "FROM futures_risk_policies r JOIN trading_execution_policies p "
                        + "ON p.instrument_id=r.instrument_id AND p.policy_version=r.policy_version "
                        + "JOIN trading_instruments i ON i.instrument_id=r.instrument_id "
                        + "WHERE r.instrument_id=? AND r.policy_version=? AND i.instrument_kind='PERPETUAL'",
                instrument, version);
        if (rows.size() != 1) throw new IllegalStateException("futures risk policy evidence is unavailable");
        return verified(rows.getFirst());
    }

    private FuturesRiskPolicy verified(Map<String, Object> row) {
        try {
            FuturesRiskPolicy risk = json.readValue(row.get("risk_policy").toString(), FuturesRiskPolicy.class);
            TradingExecutionPolicy execution = json.readValue(row.get("execution_policy").toString(),
                    TradingExecutionPolicy.class);
            if (!risk.execution().equals(execution)
                    || !risk.execution().rules().instrumentId().equals(row.get("instrument_id"))
                    || risk.execution().rules().policyVersion() != ((Number) row.get("policy_version")).longValue()
                    || !risk.execution().rules().marketSymbol().equals(row.get("market_symbol")))
                throw new IllegalStateException("futures policy identity does not match stored instrument");
            return risk;
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new IllegalStateException("futures policy JSON is invalid", invalid);
        }
    }
}
