/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.FeeCalculation;
import com.example.exchange.domain.model.entity.SymbolConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeService {

    private static final int MONEY_SCALE = 18;

    public FeeCalculation calculate(TradeExecuted trade, SymbolConfig config) {
        BigDecimal notional = trade.notional();
        BigDecimal feeRate = trade.maker()
                ? config.makerFeeRateOrDefault()
                : config.takerFeeRateOrDefault();
        BigDecimal rebateRate = trade.maker()
                ? config.makerRebateRateOrDefault()
                : config.referralRebateRateOrDefault();
        BigDecimal fee = notional.multiply(feeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal rebate = notional.multiply(rebateRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new FeeCalculation(feeRate, fee, rebateRate, rebate);
    }
}
