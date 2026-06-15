/*
 * 檔案用途：基礎設施設定，提供受保護 API 的限流、IP 白名單與審計參數。
 */
package com.example.exchange.infra.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "security-controls")
public class SecurityControlsProperties {

    /**
     * 總開關。關閉後不做 rate limit / IP allowlist / audit。
     */
    private boolean enabled = true;

    /**
     * 是否輸出安全審計 log。
     */
    private boolean auditEnabled = true;

    /**
     * 是否啟用固定視窗 rate limit。
     */
    private boolean rateLimitEnabled = true;

    /**
     * 每個 client IP + API 分類每分鐘允許的請求數。
     */
    @Min(1)
    private int requestsPerMinute = 120;

    /**
     * 最多追蹤多少個 client IP + API 分類組合，避免 in-memory limiter 無限成長。
     */
    @Min(1)
    private int maxTrackedKeys = 10000;

    /**
     * 是否啟用 IP 白名單。空白名單代表全部拒絕，因此 production 開啟前要先設定 allowlist。
     */
    private boolean ipAllowlistEnabled = false;

    /**
     * 支援精確 IP、IPv4 CIDR，例如 10.0.0.0/8；空白字串會被忽略。
     */
    private List<String> ipAllowlist = new ArrayList<>();

    /**
     * 若部署在可信反向代理後方，可從此 header 取得真實 client IP。
     */
    private String clientIpHeader = "X-Forwarded-For";

    /**
     * 會套用安全控制的 API path patterns。
     */
    private List<String> protectedPathPatterns = new ArrayList<>(List.of(
            "/api/order/**",
            "/api/margin/**",
            "/api/risk/**",
            "/api/recovery/**",
            "/api/prediction/session/**",
            "/api/prediction/orders/**",
            "/api/prediction/clob/**",
            "/api/prediction/markets/discover",
            "/api/prediction/markets/sync",
            "/api/prediction/markets/sync-reset",
            "/api/prediction/markets/sync-progress",
            "/api/prediction/markets/retry/**",
            "/api/prediction/markets/price-refresh",
            "/api/prediction/ws/**",
            "/api/prediction/approve/**",
            "/api/messages/**",
            "/api/message-preferences/**",
            "/api/admin/messages/**",
            "/api/system/messages/**"
    ));
}
