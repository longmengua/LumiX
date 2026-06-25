/*
 * 檔案用途：
 * JPA entity，對應資料表 trading_symbol。
 *
 * 白話：
 * 這張表就是後台的「交易對設定」。
 *
 * 注意：
 * 這支只描述資料表欄位。
 * 不放商業規則。
 * 不放 SymbolConfig 轉換。
 * 不呼叫 repository。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.enums.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "trading_symbol")
public class TradingSymbolRecord {

    /**
     * 資料庫流水 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 交易對代碼。
     *
     * 例如：
     * BTCUSDT-SPOT
     * BTCUSDT-PERP
     */
    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    /**
     * 商品類型。
     *
     * SPOT：現貨
     * PERPETUAL：永續合約
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private ProductType productType;

    /**
     * 前面的幣。
     *
     * 例如 BTCUSDT 裡面的 BTC。
     */
    @Column(name = "base_asset", nullable = false, length = 32)
    private String baseAsset;

    /**
     * 後面的幣。
     *
     * 例如 BTCUSDT 裡面的 USDT。
     */
    @Column(name = "quote_asset", nullable = false, length = 32)
    private String quoteAsset;

    /**
     * 保證金幣種。
     *
     * 現貨通常是 null。
     * USDT 合約通常是 USDT。
     */
    @Column(name = "margin_asset", length = 32)
    private String marginAsset;

    /**
     * 價格最小跳動單位。
     *
     * 例如 0.01。
     */
    @Column(name = "price_tick", nullable = false, precision = 38, scale = 18)
    private BigDecimal priceTick;

    /**
     * 數量最小跳動單位。
     *
     * 例如 0.001。
     */
    @Column(name = "lot_size", nullable = false, precision = 38, scale = 18)
    private BigDecimal lotSize;

    /**
     * 最小下單數量。
     *
     * 例如至少要買 0.001 BTC。
     */
    @Column(name = "min_qty", nullable = false, precision = 38, scale = 18)
    private BigDecimal minQty;

    /**
     * 最小下單金額。
     *
     * 例如至少要 5 USDT。
     */
    @Column(name = "min_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal minNotional;

    /**
     * 單筆最多可以下多少錢。
     *
     * 例如單筆最多 1,000,000 USDT。
     */
    @Column(name = "max_order_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxOrderNotional;

    /**
     * 最大持倉金額。
     *
     * 現貨通常是 0。
     * 合約用來限制倉位不要太大。
     */
    @Column(name = "max_position_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxPositionNotional;

    /**
     * 最多可以掛幾筆還沒成交的單。
     */
    @Column(name = "max_open_orders", nullable = false)
    private Integer maxOpenOrders;

    /**
     * 最大槓桿。
     *
     * 現貨固定 1。
     * 合約可以是 20、50、75、100。
     */
    @Column(name = "max_leverage", nullable = false)
    private int maxLeverage;

    /**
     * 初始保證金率。
     *
     * 現貨填 1。
     * 合約例如 100 倍就是 0.01。
     */
    @Column(name = "initial_margin_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal initialMarginRate;

    /**
     * 維持保證金率。
     *
     * 合約虧損太多時，會用這個判斷強平風險。
     */
    @Column(name = "maintenance_margin_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal maintenanceMarginRate;

    /**
     * 掛單手續費。
     */
    @Column(name = "maker_fee_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal makerFeeRate;

    /**
     * 吃單手續費。
     */
    @Column(name = "taker_fee_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal takerFeeRate;

    /**
     * 掛單返佣。
     *
     * 沒有返佣就 0。
     */
    @Column(name = "maker_rebate_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal makerRebateRate;

    /**
     * 推薦人返佣。
     *
     * 沒有推薦返佣就 0。
     */
    @Column(name = "referral_rebate_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal referralRebateRate;

    /**
     * 價格保護範圍。
     *
     * 例如 0.10 代表不能偏離參考價格 10% 以上。
     */
    @Column(name = "price_band_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal priceBandRate;

    /**
     * 是否允許交易。
     *
     * false 就不能下單。
     */
    @Column(name = "trading_enabled", nullable = false)
    private boolean tradingEnabled;

    /**
     * 前台是否顯示。
     *
     * false 代表後台有這個交易對，但前台先不露出。
     */
    @Column(name = "visible", nullable = false)
    private boolean visible;

    /**
     * 是否只允許平倉。
     *
     * true 代表不能開新倉，只能減少風險。
     */
    @Column(name = "reduce_only", nullable = false)
    private boolean reduceOnly;

    /**
     * 建立時間。
     *
     * 由資料庫自動寫入。
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    /**
     * 更新時間。
     *
     * 由資料庫自動更新。
     */
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;
}
