/*
 * 檔案用途：基礎設施設定，控制體驗金到期與使用資格規則。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "bonus-credit")
public class BonusCreditProperties {

    /**
     * 體驗金消耗資格規則。預設關閉，避免 MVP 行為被設定意外改變。
     */
    private Eligibility eligibility = new Eligibility();

    @Data
    public static class Eligibility {

        /**
         * true 時 consume 會套用 symbol / order type / expense account gate。
         */
        private boolean enabled = false;

        /**
         * 空清單代表不限制；有值時只允許列出的 symbol 使用體驗金。
         */
        private List<String> allowedSymbols = new ArrayList<>();

        /**
         * 永遠拒絕的 symbol，優先於 allowedSymbols。
         */
        private List<String> blockedSymbols = new ArrayList<>();

        /**
         * 空清單代表不限制；有值時只允許列出的 order type 使用體驗金。
         */
        private List<String> allowedOrderTypes = new ArrayList<>();

        /**
         * 空清單代表不限制；有值時只允許列出的 expense ledger account 消耗體驗金。
         */
        private List<String> allowedExpenseAccounts = new ArrayList<>();
    }
}
