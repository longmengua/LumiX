/*
 * 檔案用途：
 * JPA entity，對應資料表 trading_symbol。
 *
 * 白話：
 * 這張表就是後台的「交易對設定」。
 *
 * 例如：
 * - BTCUSDT-SPOT 要不要開放交易
 * - BTCUSDT-PERP 最大幾倍槓桿
 * - 價格最小跳多少
 * - 數量最小跳多少
 * - 手續費多少
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
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "trading_symbol")
public class TradingSymbolRecord {

    /**
     * 資料庫流水 ID。
     *
     * 沒有業務意思，只是資料庫主鍵。
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
     *
     * 這個欄位是業務唯一鍵。
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

    /**
     * 把資料庫資料轉成領域模型 SymbolConfig。
     *
     * 白話：
     * DB 是存資料用。
     * SymbolConfig 是程式下單、風控、撮合時要用的設定物件。
     */
    public SymbolConfig toSymbolConfig(List<TradingSymbolRiskTierRecord> tierRecords) {
        List<SymbolConfig.RiskTier> tiers = tierRecords == null
                ? List.of()
                : tierRecords.stream()
                .map(TradingSymbolRiskTierRecord::toRiskTier)
                .toList();

        return SymbolConfig.builder()
                .symbol(normalize(symbol))
                .productType(productType)
                .baseAsset(normalize(baseAsset))
                .quoteAsset(normalize(quoteAsset))
                .marginAsset(normalizeNullable(marginAsset))
                .priceTick(priceTick)
                .lotSize(lotSize)
                .minQty(minQty)
                .minNotional(minNotional)
                .maxOrderNotional(maxOrderNotional)
                .maxPositionNotional(maxPositionNotional)
                .maxOpenOrders(maxOpenOrders)
                .maxLeverage(maxLeverage)
                .initialMarginRate(initialMarginRate)
                .maintenanceMarginRate(maintenanceMarginRate)
                .makerFeeRate(makerFeeRate)
                .takerFeeRate(takerFeeRate)
                .makerRebateRate(makerRebateRate)
                .referralRebateRate(referralRebateRate)
                .priceBandRate(priceBandRate)
                .riskTiers(tiers)
                .tradingEnabled(tradingEnabled)
                .visible(visible)
                .reduceOnly(reduceOnly)
                .build();
    }

    /**
     * 把 SymbolConfig 轉回資料庫 entity。
     *
     * 後台新增或修改交易對時會用到。
     */
    public static TradingSymbolRecord from(SymbolConfig config) {
        TradingSymbolRecord record = new TradingSymbolRecord();

        record.setSymbol(normalize(config.getSymbol()));
        record.setProductType(config.productTypeOrDefault());
        record.setBaseAsset(normalize(config.getBaseAsset()));
        record.setQuoteAsset(normalize(config.getQuoteAsset()));
        record.setMarginAsset(normalizeNullable(config.getMarginAsset()));
        record.setPriceTick(config.priceTickOrDefault());
        record.setLotSize(config.lotSizeOrDefault());
        record.setMinQty(config.minQtyOrDefault());
        record.setMinNotional(config.minNotionalOrDefault());
        record.setMaxOrderNotional(config.maxOrderNotionalOrDefault());
        record.setMaxPositionNotional(config.maxPositionNotionalOrDefault());
        record.setMaxOpenOrders(config.maxOpenOrdersOrDefault());
        record.setMaxLeverage(config.maxLeverageOrDefault());
        record.setInitialMarginRate(config.initialMarginRateOrDefault());
        record.setMaintenanceMarginRate(config.maintenanceMarginRateOrDefault());
        record.setMakerFeeRate(config.makerFeeRateOrDefault());
        record.setTakerFeeRate(config.takerFeeRateOrDefault());
        record.setMakerRebateRate(config.makerRebateRateOrDefault());
        record.setReferralRebateRate(config.referralRebateRateOrDefault());
        record.setPriceBandRate(config.priceBandRateOrDefault());
        record.setTradingEnabled(config.isTradingEnabled());
        record.setVisible(config.visibleOrDefault());
        record.setReduceOnly(config.reduceOnlyOrDefault());

        return record;
    }

    /**
     * 統一把字串整理成大寫。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    /**
     * 可以為 null 的欄位，用這個整理。
     */
    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}