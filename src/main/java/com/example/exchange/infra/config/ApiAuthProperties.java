/*
 * 檔案用途：基礎設施設定，提供 API key 與 JWT authentication / authorization 參數。
 */
package com.example.exchange.infra.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "api-auth")
public class ApiAuthProperties {

    /**
     * 總開關。dev 可關閉，prod 預設應開啟。
     */
    private boolean enabled = false;

    /**
     * 是否允許 X-API-Key 類型憑證。
     */
    private boolean apiKeyEnabled = true;

    /**
     * API key header 名稱。
     */
    private String apiKeyHeader = "X-API-Key";

    /**
     * API key 設定格式：
     * keyId:sha256Hex:ROLE_ADMIN|ROLE_TRADER:admin|trade:write;...
     */
    private String apiKeys = "";

    /**
     * 是否允許 Authorization: Bearer JWT。
     */
    private boolean jwtEnabled = true;

    /**
     * HS256 JWT secret。prod 應由 secret manager 或環境變數注入。
     */
    private String jwtHmacSecret = "";

    /**
     * JWT exp / nbf 容忍秒數。
     */
    @Min(0)
    private long clockSkewSeconds = 60;
}
