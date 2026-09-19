package com.lumix.admin.asset;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 資產調整可用現貨幣種的受治理讀取服務。
 *
 * <p>選項直接讀取後端 assets 主檔的 ACTIVE 設定；提交時仍由 adjustment service 再次驗證，避免讀取與提交之間的設定變更造成繞過。</p>
 */
@Service
class AssetAdjustmentAssetConfigurationService {
    private final SuperAdminAccessService access;
    private final JdbcTemplate jdbcTemplate;

    AssetAdjustmentAssetConfigurationService(SuperAdminAccessService access, JdbcTemplate jdbcTemplate) {
        this.access = Objects.requireNonNull(access, "access must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    /** 只有已驗證的最高管理員可讀取可執行資產設定，避免公開列舉平台資產主檔。 */
    @Transactional(readOnly = true)
    List<AssetAdjustmentAssetOption> listActiveSpotAssets(AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        return List.copyOf(jdbcTemplate.query(
                "SELECT asset_symbol, display_name, precision_scale FROM assets WHERE status = 'ACTIVE' ORDER BY asset_symbol ASC",
                (resultSet, rowNumber) -> new AssetAdjustmentAssetOption(
                        resultSet.getString("asset_symbol"), resultSet.getString("display_name"), resultSet.getInt("precision_scale"))));
    }
}
