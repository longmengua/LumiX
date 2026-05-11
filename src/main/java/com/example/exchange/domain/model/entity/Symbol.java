package com.example.exchange.domain.model.entity;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

/**
 * 交易對值物件（Domain Model）
 *
 * 用途：
 * - 描述一個交易對（例如：BTC/USDT）的基本屬性與精度設定。
 * - 由應用層/撮合層用來做數值量化（price/qty 的小數位數）、格式化與顯示。
 *
 * 設計說明：
 * - 使用 Lombok 產生樣板碼：
 *   @Data           → 生成 getter/setter、equals、hashCode、toString
 *   @Builder        → 提供建構器（與 @Jacksonized 搭配可支援 Jackson 反序列化）
 *   @AllArgsConstructor / @NoArgsConstructor → 供 ORM/序列化工具使用
 *   @Jacksonized    → 讓 Jackson 能用 Builder 方式做 JSON 反序列化
 *
 * 精度欄位：
 * - priceScale：價格可接受的小數位數（例如 2 → 123.45）
 * - qtyScale  ：數量可接受的小數位數（例如 3 → 0.123）
 *
 * 注意事項：
 * - 「刻度（scale）」只描述小數位數，實際的「跳動步進（tick/step）」與「最小名義金額」不在此類別強制。
 * - 真正的數值量化（四捨五入/截斷）應在應用層或 Value Object（如 Money/Quantity）中統一處理。
 *
 * 常見擴充（TODO）：
 * - TODO: 新增 priceTick、qtyStep（跳動步進，例如 0.01、0.001）
 * - TODO: 新增 minQty、minNotional（最小下單量與最小名義金額）
 * - TODO: 新增 displayCode（顯示用符號，如 "BTC/USDT"）、internalCode（內部代碼，如 "BTCUSDT"）
 * - TODO: 新增合約屬性（如 linear/inverse、合約面值 contractSize、保證金資產 marginAsset）
 * - TODO: 新增交易狀態（tradingEnabled、onlyReduce、maintenance 等）
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
public class Symbol {

    /** 基礎幣（Base），例：BTC */
    private String base;

    /** 報價幣（Quote），例：USDT */
    private String quote;

    /**
     * 價格小數精度（允許多少位小數）
     * 例：2 → 123.45
     *
     * 提示：
     * - 僅描述「位數」；實際步進（tick）請另增 priceTick。
     * - 應在驗證時確保 >= 0。
     */
    private int priceScale;

    /**
     * 數量小數精度（允許多少位小數）
     * 例：3 → 0.123
     *
     * 提示：
     * - 僅描述「位數」；實際步進（step）請另增 qtyStep。
     * - 應在驗證時確保 >= 0。
     */
    private int qtyScale;

    /**
     * 內部代碼：以「Base + Quote」拼接（例：BTCUSDT）
     *
     * 注意：
     * - 大小寫規範：若系統需要統一大寫，可於此處或建構時統一 toUpperCase()。
     * - 顯示層若需要 "BTC/USDT" 可在 UI 轉換或新增 displayCode()。
     */
    public String code() {
        return (base == null ? "" : base.toUpperCase()) + (quote == null ? "" : quote.toUpperCase());
    }
}
