package com.lumix.admin.asset;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 受治理空投使用的現貨幣種設定讀取服務。
 *
 * <p>選項直接讀取後端 {@code assets} 主檔的交易所內部名稱與 ACTIVE 設定，避免 browser 維護可入帳幣種白名單；實際空投時
 * {@link AdminAirdropService} 仍會再次驗證資產狀態，避免讀取與提交之間的設定變更造成繞過。</p>
 */
@Service
class AdminAirdropAssetConfigurationService {
    private final SuperAdminAccessService access;
    private final JdbcTemplate jdbcTemplate;

    AdminAirdropAssetConfigurationService(SuperAdminAccessService access, JdbcTemplate jdbcTemplate) {
        this.access = Objects.requireNonNull(access, "access must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    /** 只有已驗證的最高管理員可讀取可執行資產設定，避免公開列舉平台資產主檔。 */
    @Transactional(readOnly = true)
    List<AdminAirdropAssetOption> listActiveSpotAssets(AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        return List.copyOf(jdbcTemplate.query(
                "SELECT asset_symbol, display_name, precision_scale FROM assets WHERE status = 'ACTIVE' ORDER BY asset_symbol ASC",
                (resultSet, rowNumber) -> new AdminAirdropAssetOption(
                        resultSet.getString("asset_symbol"),
                        resultSet.getString("display_name"),
                        resultSet.getInt("precision_scale")
                )
        ));
    }
}
