/*
 * 檔案用途：
 * 這個類別用來描述一個交易對可以怎麼交易。
 *
 * 例如：
 * BTCUSDT-SPOT 代表 BTC / USDT 現貨
 * BTCUSDT-PERP 代表 BTC / USDT 永續合約
 *
 * 它會記錄：
 * - 這個交易對是現貨還是合約
 * - 價格可以跳多少
 * - 數量可以跳多少
 * - 最小可以下多少
 * - 單筆最多可以下多少錢
 * - 合約最大可以開幾倍槓桿
 * - 手續費多少
 * - 這個交易對現在能不能交易
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.enums.ProductType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * 交易對設定。
 *
 * 簡單理解：
 * 後台新增一個 BTCUSDT-PERP，這裡就是拿來描述它的交易規則。
 */
@Data
@Builder
@Jacksonized
public class SymbolConfig {

    /**
     * 交易對代碼。
     *
     * 例如：
     * BTCUSDT-SPOT
     * ETHUSDT-SPOT
     * BNBUSDT-SPOT
     * BTCUSDT-PERP
     * ETHUSDT-PERP
     * BNBUSDT-PERP
     */
    private String symbol;

    /**
     * 商品類型。
     *
     * SPOT：現貨
     * PERPETUAL：永續合約
     */
    private ProductType productType;

    /**
     * 前面的幣。
     *
     * 例如 BTCUSDT 裡面的 BTC。
     */
    private String baseAsset;

    /**
     * 後面的幣。
     *
     * 例如 BTCUSDT 裡面的 USDT。
     */
    private String quoteAsset;

    /**
     * 保證金幣種。
     *
     * 現貨通常用不到，可以是 null。
     * USDT 合約通常會是 USDT。
     */
    private String marginAsset;

    /**
     * 價格最小跳動單位。
     *
     * 例如 priceTick = 0.01
     * 代表價格可以是：
     * 100.01
     * 100.02
     * 100.03
     *
     * 但不能是：
     * 100.001
     */
    private BigDecimal priceTick;

    /**
     * 數量最小下單單位。
     *
     * 例如 lotSize = 0.001
     * 代表數量可以是：
     * 0.001
     * 0.002
     * 0.003
     *
     * 但不能是：
     * 0.0005
     */
    private BigDecimal lotSize;

    /**
     * 最小下單數量。
     *
     * 例如 minQty = 0.01
     * 代表至少要下 0.01。
     *
     * lotSize 管「每次跳多少」。
     * minQty 管「最少要多少」。
     */
    private BigDecimal minQty;

    /**
     * 最小下單金額。
     *
     * 例如 minNotional = 5
     * 代表這筆單至少要 5 USDT。
     *
     * 計算方式：
     * 價格 * 數量
     */
    private BigDecimal minNotional;

    /**
     * 單筆最多可以下多少錢。
     *
     * 例如 maxOrderNotional = 100000
     * 代表一筆單最多 100,000 USDT。
     *
     * 這是防止使用者一次下太大的單。
     */
    private BigDecimal maxOrderNotional;

    /**
     * 這個交易對最多可以持有多少倉位。
     *
     * 現貨通常用不到。
     * 合約會用到，避免單一使用者倉位太大。
     */
    private BigDecimal maxPositionNotional;

    /**
     * 單一使用者最多可以掛幾筆未成交訂單。
     *
     * 例如 maxOpenOrders = 200
     * 代表這個交易對最多掛 200 筆還沒成交的單。
     */
    private Integer maxOpenOrders;

    /**
     * 最大槓桿。
     *
     * 現貨固定是 1。
     * 合約才可以設定成 20、50、75、100。
     */
    private int maxLeverage;

    /**
     * 掛單手續費。
     *
     * Maker 是放單到訂單簿的人。
     */
    private BigDecimal makerFeeRate;

    /**
     * 吃單手續費。
     *
     * Taker 是直接吃掉別人掛單的人。
     */
    private BigDecimal takerFeeRate;

    /**
     * 掛單返佣。
     *
     * 如果暫時沒有做返佣，設成 0 就好。
     */
    private BigDecimal makerRebateRate;

    /**
     * 推薦人返佣。
     *
     * 如果暫時沒有做邀請返佣，設成 0 就好。
     */
    private BigDecimal referralRebateRate;

    /**
     * 價格保護範圍。
     *
     * 例如 priceBandRate = 0.10
     * 代表價格不能偏離參考價格 10% 以上。
     *
     * 這是防止亂掛超離譜價格。
     */
    private BigDecimal priceBandRate;

    /**
     * 初始保證金率。
     *
     * 合約用。
     *
     * 例如 100 倍槓桿：
     * 初始保證金率大約是 0.01。
     */
    private BigDecimal initialMarginRate;

    /**
     * 維持保證金率。
     *
     * 合約用。
     *
     * 倉位虧損太多，低於這個標準時，就可能被強平。
     */
    private BigDecimal maintenanceMarginRate;

    /**
     * 合約風控分層。
     *
     * 簡單講：
     * 倉位越大，允許的槓桿通常越低。
     *
     * 例如：
     * 10 萬 USDT 以下，可以 100 倍
     * 50 萬 USDT 以下，只能 50 倍
     * 100 萬 USDT 以下，只能 20 倍
     */
    private List<RiskTier> riskTiers;

    /**
     * 這個交易對能不能交易。
     *
     * false 代表前台不能下單。
     */
    private boolean tradingEnabled;

    /**
     * 前台要不要顯示。
     *
     * 例如後台先建好交易對，但還不想讓使用者看到，可以設 false。
     */
    private Boolean visible;

    /**
     * 是否只允許平倉。
     *
     * true 代表：
     * 可以平倉、減少風險。
     * 不可以開新倉、增加風險。
     */
    private Boolean reduceOnly;

    /**
     * 轉成撮合引擎使用的 Symbol。
     *
     * 撮合引擎主要需要知道：
     * - 交易對代碼
     * - base 是誰
     * - quote 是誰
     * - 價格小數位
     * - 數量小數位
     */
    public Symbol toSymbol() {
        return Symbol.builder()
                .code(symbol)
                .base(baseAsset)
                .quote(quoteAsset)
                .priceScale(scaleOf(priceTick))
                .qtyScale(scaleOf(lotSize))
                .build();
    }

    /**
     * 如果沒有設定商品類型，先預設成永續合約。
     *
     * 正式資料最好不要省略 productType。
     */
    public ProductType productTypeOrDefault() {
        return productType == null ? ProductType.PERPETUAL : productType;
    }

    /**
     * 是否是現貨。
     */
    public boolean isSpot() {
        return productTypeOrDefault() == ProductType.SPOT;
    }

    /**
     * 是否是永續合約。
     */
    public boolean isPerpetual() {
        return productTypeOrDefault() == ProductType.PERPETUAL;
    }

    /**
     * 前台是否顯示。
     *
     * 沒設定時，預設顯示。
     */
    public boolean visibleOrDefault() {
        return visible == null || visible;
    }

    /**
     * 是否只允許平倉。
     *
     * 沒設定時，預設不是 reduce-only。
     */
    public boolean reduceOnlyOrDefault() {
        return Boolean.TRUE.equals(reduceOnly);
    }

    /**
     * 是否允許正常下單。
     *
     * 必須同時符合：
     * 1. tradingEnabled = true
     * 2. reduceOnly = false
     */
    public boolean normalTradingAllowed() {
        return tradingEnabled && !reduceOnlyOrDefault();
    }

    /**
     * 價格跳動單位。
     *
     * 沒設定時，預設 0.01。
     */
    public BigDecimal priceTickOrDefault() {
        return defaultIfNull(priceTick, new BigDecimal("0.01"));
    }

    /**
     * 數量跳動單位。
     *
     * 沒設定時，預設 0.001。
     */
    public BigDecimal lotSizeOrDefault() {
        return defaultIfNull(lotSize, new BigDecimal("0.001"));
    }

    /**
     * 最小下單數量。
     *
     * 沒設定時，預設等於 lotSize。
     */
    public BigDecimal minQtyOrDefault() {
        return defaultIfNull(minQty, lotSizeOrDefault());
    }

    /**
     * 最小下單金額。
     *
     * 沒設定時，預設 0。
     */
    public BigDecimal minNotionalOrDefault() {
        return defaultIfNull(minNotional, BigDecimal.ZERO);
    }

    /**
     * 單筆最大下單金額。
     *
     * 沒設定時，預設 1,000,000。
     */
    public BigDecimal maxOrderNotionalOrDefault() {
        return defaultIfNull(maxOrderNotional, new BigDecimal("1000000"));
    }

    /**
     * 最大持倉金額。
     *
     * 現貨不看持倉，預設 0。
     * 合約沒設定時，預設 5,000,000。
     */
    public BigDecimal maxPositionNotionalOrDefault() {
        if (isSpot()) {
            return defaultIfNull(maxPositionNotional, BigDecimal.ZERO);
        }
        return defaultIfNull(maxPositionNotional, new BigDecimal("5000000"));
    }

    /**
     * 最大槓桿。
     *
     * 現貨固定 1。
     * 合約沒設定時，預設 20。
     */
    public int maxLeverageOrDefault() {
        if (isSpot()) {
            return 1;
        }
        return maxLeverage <= 0 ? 20 : maxLeverage;
    }

    /**
     * 最大未成交掛單數。
     *
     * 沒設定時，預設 200。
     */
    public int maxOpenOrdersOrDefault() {
        return maxOpenOrders == null || maxOpenOrders <= 0 ? 200 : maxOpenOrders;
    }

    /**
     * Maker 手續費。
     *
     * 沒設定時，預設 0.0002。
     */
    public BigDecimal makerFeeRateOrDefault() {
        return defaultIfNull(makerFeeRate, new BigDecimal("0.0002"));
    }

    /**
     * Taker 手續費。
     *
     * 沒設定時，預設 0.0005。
     */
    public BigDecimal takerFeeRateOrDefault() {
        return defaultIfNull(takerFeeRate, new BigDecimal("0.0005"));
    }

    /**
     * Maker 返佣。
     *
     * 沒設定時，預設 0。
     */
    public BigDecimal makerRebateRateOrDefault() {
        return defaultIfNull(makerRebateRate, BigDecimal.ZERO);
    }

    /**
     * 推薦返佣。
     *
     * 沒設定時，預設 0。
     */
    public BigDecimal referralRebateRateOrDefault() {
        return defaultIfNull(referralRebateRate, BigDecimal.ZERO);
    }

    /**
     * 價格保護範圍。
     *
     * 沒設定時，預設 10%。
     */
    public BigDecimal priceBandRateOrDefault() {
        return defaultIfNull(priceBandRate, new BigDecimal("0.10"));
    }

    /**
     * 維持保證金率。
     *
     * 現貨不使用保證金，所以回傳 1。
     * 合約沒設定時，預設 0.005。
     */
    public BigDecimal maintenanceMarginRateOrDefault() {
        if (isSpot()) {
            return BigDecimal.ONE;
        }
        return defaultIfNull(maintenanceMarginRate, new BigDecimal("0.005"));
    }

    /**
     * 初始保證金率。
     *
     * 現貨不使用保證金，所以回傳 1。
     * 合約如果沒設定，就用最大槓桿反推。
     *
     * 例如：
     * 20 倍 => 1 / 20 = 0.05
     * 100 倍 => 1 / 100 = 0.01
     */
    public BigDecimal initialMarginRateOrDefault() {
        if (isSpot()) {
            return BigDecimal.ONE;
        }
        return defaultIfNull(initialMarginRate, leverageInitialMarginRate(maxLeverageOrDefault()));
    }

    /**
     * 用持倉金額找出適用的風控分層。
     *
     * 現貨不用風控分層，直接回傳 null。
     */
    public RiskTier riskTierForNotional(BigDecimal positionNotional) {
        if (isSpot()) {
            return null;
        }

        BigDecimal notional = positionNotional == null ? BigDecimal.ZERO : positionNotional.abs();
        List<RiskTier> tiers = configuredRiskTiers();

        if (tiers.isEmpty()) {
            return defaultRiskTier();
        }

        return tiers.stream()
                .filter(tier -> tier.maxPositionNotionalOrDefault(maxPositionNotionalOrDefault()).compareTo(notional) >= 0)
                .min(Comparator.comparing(tier -> tier.maxPositionNotionalOrDefault(maxPositionNotionalOrDefault())))
                .orElse(null);
    }

    /**
     * 用持倉金額取得初始保證金率。
     */
    public BigDecimal initialMarginRateForNotional(BigDecimal positionNotional) {
        RiskTier tier = riskTierForNotional(positionNotional);
        return tier == null ? initialMarginRateOrDefault() : tier.initialMarginRateOrDefault(initialMarginRateOrDefault());
    }

    /**
     * 用持倉金額取得維持保證金率。
     */
    public BigDecimal maintenanceMarginRateForNotional(BigDecimal positionNotional) {
        RiskTier tier = riskTierForNotional(positionNotional);
        return tier == null ? maintenanceMarginRateOrDefault() : tier.maintenanceMarginRateOrDefault(maintenanceMarginRateOrDefault());
    }

    /**
     * 用持倉金額取得最大槓桿。
     *
     * 現貨固定 1。
     * 合約會先看 risk tier。
     */
    public int maxLeverageForNotional(BigDecimal positionNotional) {
        if (isSpot()) {
            return 1;
        }

        RiskTier tier = riskTierForNotional(positionNotional);
        return tier == null ? maxLeverageOrDefault() : tier.maxLeverageOrDefault(maxLeverageOrDefault());
    }

    /**
     * 檢查價格是否合法。
     *
     * 例如 priceTick = 0.01：
     * 100.01 合法
     * 100.001 不合法
     */
    public boolean isPriceAligned(BigDecimal price) {
        return isAligned(price, priceTickOrDefault());
    }

    /**
     * 檢查數量是否合法。
     *
     * 例如 lotSize = 0.001：
     * 0.001 合法
     * 0.0015 不合法
     */
    public boolean isQtyAligned(BigDecimal qty) {
        return isAligned(qty, lotSizeOrDefault());
    }

    /**
     * 取得有效的風控分層。
     *
     * 會排除掉沒有設定最大持倉金額的分層。
     */
    private List<RiskTier> configuredRiskTiers() {
        if (riskTiers == null || riskTiers.isEmpty()) {
            return List.of();
        }

        return riskTiers.stream()
                .filter(tier -> tier != null
                        && tier.getMaxPositionNotional() != null
                        && tier.getMaxPositionNotional().signum() > 0)
                .sorted(Comparator.comparing(tier -> tier.maxPositionNotionalOrDefault(maxPositionNotionalOrDefault())))
                .toList();
    }

    /**
     * 預設風控分層。
     *
     * 如果後台沒有設定 risk tier，就用這個當 fallback。
     */
    private RiskTier defaultRiskTier() {
        return RiskTier.builder()
                .tier(1)
                .maxPositionNotional(maxPositionNotionalOrDefault())
                .initialMarginRate(initialMarginRateOrDefault())
                .maintenanceMarginRate(maintenanceMarginRateOrDefault())
                .maxLeverage(maxLeverageOrDefault())
                .build();
    }

    /**
     * 算出小數位數。
     *
     * 例如：
     * 0.01 => 2 位
     * 0.001 => 3 位
     */
    private static int scaleOf(BigDecimal value) {
        if (value == null) {
            return 8;
        }
        return Math.max(0, value.stripTrailingZeros().scale());
    }

    /**
     * 如果 value 是 null，就回傳 fallback。
     */
    private static BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 用槓桿算出初始保證金率。
     *
     * 例如：
     * 20 倍 => 0.05
     * 100 倍 => 0.01
     */
    private static BigDecimal leverageInitialMarginRate(int leverage) {
        return BigDecimal.ONE.divide(BigDecimal.valueOf(Math.max(1, leverage)), 18, RoundingMode.HALF_UP);
    }

    /**
     * 檢查 value 是否是 step 的整數倍。
     *
     * 例如：
     * value = 0.003
     * step = 0.001
     * 結果：合法
     *
     * value = 0.0035
     * step = 0.001
     * 結果：不合法
     */
    private static boolean isAligned(BigDecimal value, BigDecimal step) {
        if (value == null || step == null || step.signum() <= 0) {
            return false;
        }

        return value.remainder(step).compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 合約風控分層。
     *
     * 白話：
     * 倉位越大，平台通常不會讓你開太高槓桿。
     */
    @Data
    @Builder
    @Jacksonized
    public static class RiskTier {

        /**
         * 第幾層。
         *
         * 例如：
         * 1、2、3。
         */
        private Integer tier;

        /**
         * 這一層最多可以持有多少倉位金額。
         */
        private BigDecimal maxPositionNotional;

        /**
         * 這一層的初始保證金率。
         */
        private BigDecimal initialMarginRate;

        /**
         * 這一層的維持保證金率。
         */
        private BigDecimal maintenanceMarginRate;

        /**
         * 這一層最多可以開幾倍槓桿。
         */
        private Integer maxLeverage;

        /**
         * 最大持倉金額。
         *
         * 沒設定時，使用外面傳進來的 fallback。
         */
        public BigDecimal maxPositionNotionalOrDefault(BigDecimal fallback) {
            return defaultIfNull(maxPositionNotional, fallback);
        }

        /**
         * 初始保證金率。
         *
         * 沒設定時，使用外面傳進來的 fallback。
         */
        public BigDecimal initialMarginRateOrDefault(BigDecimal fallback) {
            return defaultIfNull(initialMarginRate, fallback);
        }

        /**
         * 維持保證金率。
         *
         * 沒設定時，使用外面傳進來的 fallback。
         */
        public BigDecimal maintenanceMarginRateOrDefault(BigDecimal fallback) {
            return defaultIfNull(maintenanceMarginRate, fallback);
        }

        /**
         * 最大槓桿。
         *
         * 沒設定時，使用外面傳進來的 fallback。
         */
        public int maxLeverageOrDefault(int fallback) {
            return maxLeverage == null || maxLeverage <= 0 ? fallback : maxLeverage;
        }
    }
}