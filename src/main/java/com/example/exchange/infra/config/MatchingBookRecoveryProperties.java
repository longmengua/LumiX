/*
 * 檔案用途：基礎設施設定，控制 REST app 啟動時的 in-memory matching book 恢復。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "matching-book-recovery")
public class MatchingBookRecoveryProperties {

    /**
     * true 時 REST app 啟動後會重建本機 in-memory book，避免 Redis open orders 與 depth 分裂。
     */
    private boolean enabled = true;

    /**
     * 明確指定要恢復的 symbol；空清單時會使用 configured symbol config 清單。
     */
    private List<String> symbols = new ArrayList<>();

    /**
     * durable replay 後若 book 仍空，是否用 OrderRepository 的 open orders 作為 MVP fallback。
     */
    private boolean openOrderFallbackEnabled = true;
}
