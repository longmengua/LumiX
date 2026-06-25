/*
 * 檔案用途：
 * 用 JPA 從 trading_symbol / trading_symbol_risk_tier 查出交易對設定。
 *
 * 白話：
 * SymbolConfigRepository 說「我要 SymbolConfig」。
 * 這支負責真的去 DB 查，然後交給 SymbolConfig 做轉換。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.model.entity.TradingSymbolRecord;
import com.example.exchange.domain.model.entity.TradingSymbolRiskTierRecord;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Primary
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaSymbolConfigRepository implements SymbolConfigRepository {

    private final TradingSymbolRecordJpaRepository symbolRepository;
    private final TradingSymbolRiskTierRecordJpaRepository riskTierRepository;

    /**
     * 用交易對查設定。
     *
     * 例如：
     * BTCUSDT-SPOT
     * BTCUSDT-PERP
     */
    @Override
    public Optional<SymbolConfig> findBySymbol(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        return symbolRepository.findBySymbol(normalizedSymbol)
                .map(record -> {
                    List<TradingSymbolRiskTierRecord> tiers =
                            riskTierRepository.findBySymbolOrderByMaxPositionNotionalAsc(normalizedSymbol);

                    return SymbolConfig.from(record, tiers);
                });
    }

    /**
     * 查全部交易對設定。
     *
     * 會一次查出所有 risk tiers，避免每個交易對都查一次 DB。
     */
    @Override
    public List<SymbolConfig> findAll() {
        List<TradingSymbolRecord> symbols = symbolRepository.findAll();

        if (symbols.isEmpty()) {
            return List.of();
        }

        List<String> symbolCodes = symbols.stream()
                .map(TradingSymbolRecord::getSymbol)
                .map(JpaSymbolConfigRepository::normalizeSymbol)
                .distinct()
                .toList();

        Map<String, List<TradingSymbolRiskTierRecord>> tiersBySymbol =
                riskTierRepository.findBySymbolInOrderBySymbolAscMaxPositionNotionalAsc(symbolCodes)
                        .stream()
                        .collect(Collectors.groupingBy(
                                tier -> normalizeSymbol(tier.getSymbol())
                        ));

        return symbols.stream()
                .map(record -> SymbolConfig.from(
                        record,
                        tiersBySymbol.getOrDefault(normalizeSymbol(record.getSymbol()), List.of())
                ))
                .toList();
    }

    /**
     * Persist a symbol config back to the database.
     *
     * The market-rule update path edits only the trading-rule slice, but the JPA adapter
     * persists the full aggregate so that product type, risk tiers, and fee snapshots stay in sync.
     */
    @Override
    @Transactional
    public SymbolConfig save(SymbolConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("symbol config is required");
        }

        String normalizedSymbol = normalizeSymbol(config.getSymbol());
        if (normalizedSymbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }

        TradingSymbolRecord record = symbolRepository.findBySymbol(normalizedSymbol)
                .orElseGet(TradingSymbolRecord::new);
        applySymbolRecord(record, config, normalizedSymbol);
        symbolRepository.save(record);

        riskTierRepository.deleteBySymbol(normalizedSymbol);
        List<TradingSymbolRiskTierRecord> tiers = config.getRiskTiers() == null
                ? List.of()
                : config.getRiskTiers().stream()
                        .map(tier -> toRiskTierRecord(normalizedSymbol, tier))
                        .toList();
        if (!tiers.isEmpty()) {
            riskTierRepository.saveAll(tiers);
        }

        return SymbolConfig.from(record, tiers);
    }

    /**
     * 查指定交易對清單。
     *
     * 目前不是介面必要方法，但後面做後台或前台查詢會用得到。
     */
    public List<SymbolConfig> findBySymbols(Collection<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }

        List<String> normalizedSymbols = symbols.stream()
                .map(JpaSymbolConfigRepository::normalizeSymbol)
                .distinct()
                .toList();

        List<TradingSymbolRecord> records = symbolRepository.findAll()
                .stream()
                .filter(record -> normalizedSymbols.contains(normalizeSymbol(record.getSymbol())))
                .toList();

        if (records.isEmpty()) {
            return List.of();
        }

        Map<String, List<TradingSymbolRiskTierRecord>> tiersBySymbol =
                riskTierRepository.findBySymbolInOrderBySymbolAscMaxPositionNotionalAsc(normalizedSymbols)
                        .stream()
                        .collect(Collectors.groupingBy(
                                tier -> normalizeSymbol(tier.getSymbol())
                        ));

        return records.stream()
                .map(record -> SymbolConfig.from(
                        record,
                        tiersBySymbol.getOrDefault(normalizeSymbol(record.getSymbol()), List.of())
                ))
                .toList();
    }

    /**
     * 統一整理交易對代碼。
     */
    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    /**
     * Copy the DTO back into the JPA row without moving conversion logic into the entity class.
     */
    private static void applySymbolRecord(TradingSymbolRecord record, SymbolConfig config, String symbol) {
        record.setSymbol(symbol);
        record.setProductType(config.productTypeOrDefault());
        record.setBaseAsset(config.getBaseAsset());
        record.setQuoteAsset(config.getQuoteAsset());
        record.setMarginAsset(config.getMarginAsset());
        record.setPriceTick(config.getPriceTick());
        record.setLotSize(config.getLotSize());
        record.setMinQty(config.getMinQty());
        record.setMinNotional(config.getMinNotional());
        record.setMaxOrderNotional(config.getMaxOrderNotional());
        record.setMaxPositionNotional(config.getMaxPositionNotional());
        record.setMaxOpenOrders(config.getMaxOpenOrders());
        record.setMaxLeverage(config.getMaxLeverage());
        record.setMakerFeeRate(config.getMakerFeeRate());
        record.setTakerFeeRate(config.getTakerFeeRate());
        record.setMakerRebateRate(config.getMakerRebateRate());
        record.setReferralRebateRate(config.getReferralRebateRate());
        record.setPriceBandRate(config.getPriceBandRate());
        record.setInitialMarginRate(config.getInitialMarginRate());
        record.setMaintenanceMarginRate(config.getMaintenanceMarginRate());
        record.setTradingEnabled(config.isTradingEnabled());
        record.setVisible(config.getVisible() != null && config.getVisible());
        record.setReduceOnly(config.getReduceOnly() != null && config.getReduceOnly());
    }

    /**
     * Persist one tier row with the symbol normalized once at the repository boundary.
     */
    private static TradingSymbolRiskTierRecord toRiskTierRecord(String symbol, SymbolConfig.RiskTier tier) {
        TradingSymbolRiskTierRecord record = new TradingSymbolRiskTierRecord();
        record.setSymbol(symbol);
        record.setTier(tier.getTier());
        record.setMaxPositionNotional(tier.getMaxPositionNotional());
        record.setMaxLeverage(tier.getMaxLeverage());
        record.setInitialMarginRate(tier.getInitialMarginRate());
        record.setMaintenanceMarginRate(tier.getMaintenanceMarginRate());
        return record;
    }
}
