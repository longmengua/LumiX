package com.lumix.trading.liquidity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.trading.runtime.TradingDecimal;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 只管理報價預覽費率，不改變現有委託政策、帳本或交易啟用狀態。HUMAN_REVIEW_REQUIRED。
 * 每版保存可重算的字串費率，防止管理頁 JavaScript Number 改寫金融精度。
 */
@Service
@Profile("infrastructure")
public class ExternalQuotePreviewPolicyService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final SuperAdminAccessService access;

    public ExternalQuotePreviewPolicyService(JdbcTemplate jdbc, ObjectMapper json, SuperAdminAccessService access) {
        this.jdbc = jdbc;
        this.json = json;
        this.access = access;
    }

    public record Configuration(long version, String source, String platformRate,
                                String platformFixedUsdt, Map<String, String> assetAdjustments,
                                Map<String, String> assetFixedAdjustmentsUsdt) { }

    /** 未曾提交時回傳 empty；公開讀取只供顯示預覽，沒有可成交承諾。 */
    public Optional<Configuration> current() {
        List<String> rows = jdbc.query("SELECT policy::text FROM external_quote_preview_policies "
                + "ORDER BY policy_version DESC LIMIT 1", (rs, row) -> rs.getString(1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(decode(rows.getFirst()));
    }

    /** 最高管理員逐版提交；同版相同內容可安全重送，任何不同內容拒絕覆寫。 */
    @Transactional
    public Configuration publish(AuthenticatedUser actor, Configuration submitted) {
        access.requireActiveSuperAdmin(actor);
        Configuration policy = canonical(submitted);
        // 鎖住政策表的版本序列；不能讓兩位管理員同時以同一版本建立不同報價依據。
        jdbc.execute("LOCK TABLE external_quote_preview_policies IN EXCLUSIVE MODE");
        List<String> prior = jdbc.query("SELECT policy::text FROM external_quote_preview_policies "
                + "WHERE policy_version=?", (rs, row) -> rs.getString(1), policy.version());
        if (!prior.isEmpty()) {
            Configuration saved = decode(prior.getFirst());
            if (!saved.equals(policy)) throw new IllegalArgumentException("quote preview policy version already exists");
            return saved;
        }
        Long latest = jdbc.queryForObject("SELECT COALESCE(MAX(policy_version),0) "
                + "FROM external_quote_preview_policies", Long.class);
        if (latest == null || policy.version() != latest + 1)
            throw new IllegalArgumentException("quote preview policy version must be sequential");
        String body = encode(policy);
        jdbc.update("INSERT INTO external_quote_preview_policies(policy_version,source,policy,created_by) "
                + "VALUES (?,'BINANCE',CAST(? AS jsonb),?)", policy.version(), body, actor.userId());
        String request = "external-quote-preview:" + policy.version();
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) "
                + "VALUES ('ADMIN',?,'EXTERNAL_QUOTE_PREVIEW_POLICY_PUBLISHED','QUOTE_PREVIEW_POLICY',?,?,'SUCCESS',?)",
                actor.userId(), Long.toString(policy.version()), request, body);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) "
                + "VALUES ('QUOTE_PREVIEW_POLICY',?,'EXTERNAL_QUOTE_PREVIEW_POLICY_PUBLISHED',?,'READY',?)",
                Long.toString(policy.version()), body, request);
        return policy;
    }

    private static Configuration canonical(Configuration input) {
        Objects.requireNonNull(input, "quote preview policy is required");
        Map<String, String> rateInput = input.assetAdjustments() == null ? Map.of() : input.assetAdjustments();
        Map<String, String> fixedInput = input.assetFixedAdjustmentsUsdt() == null
                ? Map.of() : input.assetFixedAdjustmentsUsdt();
        if (input.version() <= 0 || !"BINANCE".equals(input.source())
                || rateInput.size() > 32 || fixedInput.size() > 32)
            throw new IllegalArgumentException("invalid quote preview policy");
        BigDecimal base = TradingDecimal.signed(input.platformRate());
        BigDecimal baseFixed = TradingDecimal.signed(input.platformFixedUsdt() == null ? "0" : input.platformFixedUsdt());
        new ExternalQuotePolicy(input.version(), "BTC", base, baseFixed, BigDecimal.ZERO, BigDecimal.ZERO);
        Map<String, String> rates = new TreeMap<>();
        Map<String, String> fixed = new TreeMap<>();
        java.util.Set<String> assets = new java.util.TreeSet<>(rateInput.keySet());
        assets.addAll(fixedInput.keySet());
        if (assets.size() > 32) throw new IllegalArgumentException("too many asset adjustments");
        for (String asset : assets) {
            BigDecimal rateAdjustment = TradingDecimal.signed(rateInput.getOrDefault(asset, "0"));
            BigDecimal fixedAdjustment = TradingDecimal.signed(fixedInput.getOrDefault(asset, "0"));
            new ExternalQuotePolicy(input.version(), asset, base, baseFixed, rateAdjustment, fixedAdjustment);
            rates.put(asset, rateAdjustment.toPlainString());
            fixed.put(asset, fixedAdjustment.toPlainString());
        }
        return new Configuration(input.version(), "BINANCE", base.toPlainString(), baseFixed.toPlainString(),
                Map.copyOf(rates), Map.copyOf(fixed));
    }

    private Configuration decode(String text) {
        try { return canonical(json.readValue(text, Configuration.class)); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("stored quote preview policy is invalid", exception); }
    }

    private String encode(Configuration policy) {
        try { return json.writeValueAsString(policy); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("quote preview policy encoding failed", exception); }
    }
}
