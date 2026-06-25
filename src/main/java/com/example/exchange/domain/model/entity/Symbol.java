/*
 * 檔案用途：
 * 這個類別用來描述「一個交易對」。
 *
 * 白話一點：
 * Symbol 只回答一件事：
 * 這個交易對是誰？
 *
 * 例如：
 * BTCUSDT-SPOT
 * ETHUSDT-SPOT
 * BNBUSDT-SPOT
 * BTCUSDT-PERP
 *
 * 注意：
 * 這裡不要放槓桿、手續費、最小下單金額、交易開關。
 * 那些是交易規則，應該放在 SymbolConfig。
 *
 * SymbolConfig 是來源，Symbol 是衍生結果。
 */
package com.example.exchange.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * 交易對。
 *
 * 簡單理解：
 * - base 是你要買賣的幣，例如 BTC
 * - quote 是用來計價的幣，例如 USDT
 * - code 是系統內部用的交易對代碼，例如 BTCUSDT-SPOT
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
public class Symbol {
    /**
     * 系統內部使用的交易對代碼。
     *
     * 建議格式：
     * - BTCUSDT-SPOT：BTC / USDT 現貨
     * - BTCUSDT-PERP：BTC / USDT 永續合約
     *
     * 為什麼不要只用 BTCUSDT？
     * 因為 BTCUSDT 現貨和 BTCUSDT 合約是兩個不同產品。
     * 它們的訂單簿、帳務、槓桿、風控都不一樣。
     */
    private String code;

    /**
     * 前面的幣。
     *
     * 例如：
     * BTCUSDT 裡面的 BTC。
     */
    private String base;

    /**
     * 後面的幣。
     *
     * 例如：
     * BTCUSDT 裡面的 USDT。
     */
    private String quote;

    /**
     * 價格小數位數。
     *
     * 例如 priceScale = 2：
     * 代表價格最多到小數 2 位。
     *
     * 例如：
     * 100.12 可以
     * 100.123 不可以
     *
     * 注意：
     * 這裡只記「小數位數」。
     * 真正價格要跳多少，例如 0.01、0.1，要看 SymbolConfig.priceTick。
     */
    private int priceScale;

    /**
     * 數量小數位數。
     *
     * 例如 qtyScale = 3：
     * 代表數量最多到小數 3 位。
     *
     * 例如：
     * 0.123 可以
     * 0.1234 不可以
     *
     * 注意：
     * 這裡只記「小數位數」。
     * 真正數量每次跳多少，例如 0.001、0.0001，要看 SymbolConfig.lotSize。
     */
    private int qtyScale;

    /**
     * 回傳乾淨版的交易對代碼。
     *
     * 如果 code 有值，就用 code。
     * 如果 code 沒值，就用 base + quote 組出來。
     *
     * 例如：
     * code = BTCUSDT-PERP => 回傳 BTCUSDT-PERP
     * code 沒填、base = BTC、quote = USDT => 回傳 BTCUSDT
     */
    public String code() {
        if (code != null && !code.isBlank()) {
            return code.trim().toUpperCase();
        }

        return normalizedBase() + normalizedQuote();
    }

    /**
     * 顯示用交易對名稱。
     *
     * 給前端或 log 顯示時比較好讀。
     *
     * 例如：
     * base = BTC
     * quote = USDT
     * 回傳 BTC/USDT
     */
    public String displayCode() {
        String normalizedBase = normalizedBase();
        String normalizedQuote = normalizedQuote();

        if (normalizedBase.isBlank() && normalizedQuote.isBlank()) {
            return code();
        }

        if (normalizedBase.isBlank()) {
            return normalizedQuote;
        }

        if (normalizedQuote.isBlank()) {
            return normalizedBase;
        }

        return normalizedBase + "/" + normalizedQuote;
    }

    /**
     * 乾淨版 base。
     *
     * 例如：
     * " btc " => "BTC"
     */
    public String normalizedBase() {
        return normalizeAsset(base);
    }

    /**
     * 乾淨版 quote。
     *
     * 例如：
     * " usdt " => "USDT"
     */
    public String normalizedQuote() {
        return normalizeAsset(quote);
    }

    /**
     * 價格小數位數。
     *
     * 如果設定成負數，直接當成 0。
     */
    public int priceScaleOrDefault() {
        return Math.max(0, priceScale);
    }

    /**
     * 數量小數位數。
     *
     * 如果設定成負數，直接當成 0。
     */
    public int qtyScaleOrDefault() {
        return Math.max(0, qtyScale);
    }

    /**
     * 判斷這個 Symbol 是否有基本資料。
     *
     * 至少要有：
     * - code
     * 或
     * - base + quote
     */
    public boolean hasIdentity() {
        if (code != null && !code.isBlank()) {
            return true;
        }

        return base != null && !base.isBlank()
                && quote != null && !quote.isBlank();
    }

    /**
     * 把幣種代碼整理成大寫。
     *
     * 例如：
     * "btc" => "BTC"
     * " usdt " => "USDT"
     */
    private static String normalizeAsset(String asset) {
        return asset == null ? "" : asset.trim().toUpperCase();
    }
}