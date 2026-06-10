/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.FeeCalculation;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.SymbolConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeService {

    private static final int MONEY_SCALE = 18;

    public FeeCalculation calculate(TradeExecuted trade, SymbolConfig config, Order order) {
        BigDecimal notional = trade.notional();
        // Existing resting orders keep their fee snapshot even when admins change symbol-level rates later.
        BigDecimal feeRate = trade.maker()
                ? makerRate(config, order)
                : takerRate(config, order);
        BigDecimal rebateRate = trade.maker()
                ? config.makerRebateRateOrDefault()
                : config.referralRebateRateOrDefault();
        BigDecimal fee = notional.multiply(feeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal rebate = notional.multiply(rebateRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new FeeCalculation(feeRate, fee, rebateRate, rebate);
    }

    public FeeCalculation calculate(TradeExecuted trade, SymbolConfig config) {
        return calculate(trade, config, null);
    }

    private static BigDecimal makerRate(SymbolConfig config, Order order) {
        return order == null ? config.makerFeeRateOrDefault() : order.makerFeeRateSnapshotOrDefault(config);
    }

    private static BigDecimal takerRate(SymbolConfig config, Order order) {
        return order == null ? config.takerFeeRateOrDefault() : order.takerFeeRateSnapshotOrDefault(config);
    }
}
