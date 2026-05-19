/*
 * 檔案用途：基礎設施設定，建立 Spring Bean 並連接 Kafka、Redis、Web3j 或 HTTP client。
 */
package com.example.exchange.infra.config;

import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DefaultSymbolConfigRepository implements SymbolConfigRepository {

    private final Map<String, SymbolConfig> configs = new ConcurrentHashMap<>();

    public DefaultSymbolConfigRepository() {
        put(defaultPerp("BTCUSDT", "BTC", "USDT", "0.10", "0.001", "5", "1000000", "5000000", 125));
        put(defaultPerp("ETHUSDT", "ETH", "USDT", "0.01", "0.001", "5", "500000", "3000000", 100));
        put(defaultPerp("BTCUSDT-PERP", "BTC", "USDT", "0.10", "0.001", "5", "1000000", "5000000", 125));
    }

    @Override
    public Optional<SymbolConfig> findBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        String normalized = symbol.trim().toUpperCase();
        SymbolConfig config = configs.get(normalized);
        if (config != null) return Optional.of(config);
        if (normalized.endsWith("USDT")) {
            String base = normalized.substring(0, normalized.length() - 4);
            return Optional.of(defaultPerp(normalized, base, "USDT", "0.01", "0.001", "5", "100000", "500000", 20));
        }
        return Optional.empty();
    }

    private void put(SymbolConfig config) {
        configs.put(config.getSymbol().toUpperCase(), config);
    }

    private static SymbolConfig defaultPerp(
            String symbol,
            String base,
            String quote,
            String priceTick,
            String lotSize,
            String minNotional,
            String maxOrderNotional,
            String maxPositionNotional,
            int maxLeverage
    ) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .baseAsset(base)
                .quoteAsset(quote)
                .priceTick(new BigDecimal(priceTick))
                .lotSize(new BigDecimal(lotSize))
                .minQty(new BigDecimal(lotSize))
                .minNotional(new BigDecimal(minNotional))
                .maxOrderNotional(new BigDecimal(maxOrderNotional))
                .maxPositionNotional(new BigDecimal(maxPositionNotional))
                .maxOpenOrders(200)
                .maxLeverage(maxLeverage)
                .initialMarginRate(initialMarginRate(maxLeverage))
                .makerFeeRate(new BigDecimal("0.0002"))
                .takerFeeRate(new BigDecimal("0.0005"))
                .makerRebateRate(BigDecimal.ZERO)
                .referralRebateRate(new BigDecimal("0.00005"))
                .priceBandRate(new BigDecimal("0.10"))
                .maintenanceMarginRate(new BigDecimal("0.005"))
                .riskTiers(defaultRiskTiers(maxPositionNotional, maxLeverage))
                .tradingEnabled(true)
                .build();
    }

    private static List<SymbolConfig.RiskTier> defaultRiskTiers(String maxPositionNotional, int maxLeverage) {
        BigDecimal cap = new BigDecimal(maxPositionNotional);
        return List.of(
                riskTier(1, cap.min(new BigDecimal("50000")), initialMarginRate(maxLeverage), "0.005", maxLeverage),
                riskTier(2, cap.min(new BigDecimal("250000")), "0.020", "0.010", Math.min(maxLeverage, 50)),
                riskTier(3, cap, "0.050", "0.025", Math.min(maxLeverage, 20))
        );
    }

    private static SymbolConfig.RiskTier riskTier(
            int tier,
            BigDecimal maxPositionNotional,
            BigDecimal initialMarginRate,
            String maintenanceMarginRate,
            int maxLeverage
    ) {
        return riskTier(tier, maxPositionNotional, initialMarginRate.toPlainString(), maintenanceMarginRate, maxLeverage);
    }

    private static SymbolConfig.RiskTier riskTier(
            int tier,
            BigDecimal maxPositionNotional,
            String initialMarginRate,
            String maintenanceMarginRate,
            int maxLeverage
    ) {
        return SymbolConfig.RiskTier.builder()
                .tier(tier)
                .maxPositionNotional(maxPositionNotional)
                .initialMarginRate(new BigDecimal(initialMarginRate))
                .maintenanceMarginRate(new BigDecimal(maintenanceMarginRate))
                .maxLeverage(maxLeverage)
                .build();
    }

    private static BigDecimal initialMarginRate(int maxLeverage) {
        return BigDecimal.ONE.divide(BigDecimal.valueOf(Math.max(1, maxLeverage)), 18, RoundingMode.HALF_UP);
    }
}
