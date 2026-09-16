package com.lumix.admin.asset;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 現貨幣種主檔的受治理管理服務。
 *
 * <p>新增幣種一律先以 HALTED 建立，需由最高管理員明確啟用；每次建立或狀態異動都在同一交易內留下
 * immutable audit evidence。此服務不處理錢包、私鑰、鏈上或餘額寫入。</p>
 */
@Service
class AdminSpotAssetConfigurationService {
    private static final Pattern ASSET_SYMBOL = Pattern.compile("[A-Z0-9]{2,32}");
    private final SuperAdminAccessService access;
    private final JdbcTemplate jdbcTemplate;

    AdminSpotAssetConfigurationService(SuperAdminAccessService access, JdbcTemplate jdbcTemplate) {
        this.access = Objects.requireNonNull(access, "access must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Transactional(readOnly = true)
    List<AdminSpotAssetConfiguration> list(AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        return List.copyOf(jdbcTemplate.query(
                "SELECT asset_symbol, display_name, precision_scale, status, updated_at FROM assets ORDER BY asset_symbol ASC",
                (resultSet, rowNumber) -> new AdminSpotAssetConfiguration(
                        resultSet.getString("asset_symbol"), resultSet.getString("display_name"),
                        resultSet.getInt("precision_scale"), resultSet.getString("status"),
                        resultSet.getTimestamp("updated_at").toInstant()
                )
        ));
    }

    @Transactional
    AdminSpotAssetConfiguration create(AuthenticatedUser actor, String requestedSymbol, String requestedInternalName, int precisionScale) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        String assetSymbol = canonicalSymbol(requestedSymbol);
        String internalName = canonicalInternalName(requestedInternalName);
        if (precisionScale < 0 || precisionScale > 18) throw new IllegalArgumentException("precisionScale must be between 0 and 18");
        Integer existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assets WHERE asset_symbol = ?", Integer.class, assetSymbol);
        if (existing != null && existing > 0) throw new IllegalArgumentException("asset symbol is already configured");

        jdbcTemplate.update("INSERT INTO assets (asset_symbol, display_name, precision_scale, status) VALUES (?, ?, ?, 'HALTED')",
                assetSymbol, internalName, precisionScale);
        appendAudit(actor, "ADMIN_SPOT_ASSET_CREATED", assetSymbol,
                "internalName=" + internalName + ";precisionScale=" + precisionScale + ";status=HALTED");
        return findRequired(assetSymbol);
    }

    @Transactional
    AdminSpotAssetConfiguration updateStatus(AuthenticatedUser actor, String requestedSymbol, String requestedStatus) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        String assetSymbol = canonicalSymbol(requestedSymbol);
        String status = canonicalMutableStatus(requestedStatus);
        int changed = jdbcTemplate.update("UPDATE assets SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE asset_symbol = ?", status, assetSymbol);
        if (changed != 1) throw new IllegalArgumentException("asset symbol is not configured");
        appendAudit(actor, "ADMIN_SPOT_ASSET_STATUS_CHANGED", assetSymbol, "status=" + status);
        return findRequired(assetSymbol);
    }

    private AdminSpotAssetConfiguration findRequired(String assetSymbol) {
        return jdbcTemplate.queryForObject(
                "SELECT asset_symbol, display_name, precision_scale, status, updated_at FROM assets WHERE asset_symbol = ?", (resultSet, rowNumber) ->
                        new AdminSpotAssetConfiguration(resultSet.getString("asset_symbol"), resultSet.getString("display_name"),
                                resultSet.getInt("precision_scale"), resultSet.getString("status"), resultSet.getTimestamp("updated_at").toInstant()), assetSymbol
        );
    }

    private void appendAudit(AuthenticatedUser actor, String actionType, String assetSymbol, String reason) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason) VALUES ('ADMIN', ?, ?, 'ASSET', ?, ?, 'SUCCESS', ?)",
                actor.userId(), actionType, assetSymbol, "asset-config-" + UUID.randomUUID(), reason
        );
    }

    private static String canonicalSymbol(String requestedSymbol) {
        String symbol = Objects.requireNonNull(requestedSymbol, "assetSymbol must not be null").trim().toUpperCase(Locale.ROOT);
        if (!ASSET_SYMBOL.matcher(symbol).matches()) throw new IllegalArgumentException("assetSymbol must be 2-32 uppercase letters or digits");
        return symbol;
    }

    /** 資產主檔既有 display_name 欄位只保存交易所內部名稱，鏈上名稱必須由未來的網路配置另存。 */
    private static String canonicalInternalName(String requestedInternalName) {
        String internalName = Objects.requireNonNull(requestedInternalName, "internalName must not be null").trim();
        if (internalName.isEmpty() || internalName.length() > 128) throw new IllegalArgumentException("internalName must be 1-128 characters");
        return internalName;
    }

    private static String canonicalMutableStatus(String requestedStatus) {
        String status = Objects.requireNonNull(requestedStatus, "status must not be null").trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status) && !"HALTED".equals(status)) throw new IllegalArgumentException("status must be ACTIVE or HALTED");
        return status;
    }
}
