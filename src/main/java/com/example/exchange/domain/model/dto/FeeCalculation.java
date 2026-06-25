/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class FeeCalculation {

    private final BigDecimal feeRate;

    private final BigDecimal fee;

    private final BigDecimal rebateRate;

    private final BigDecimal rebate;
    public FeeCalculation(BigDecimal feeRate, BigDecimal fee, BigDecimal rebateRate, BigDecimal rebate) {
        this.feeRate = feeRate;
        this.fee = fee;
        this.rebateRate = rebateRate;
        this.rebate = rebate;
    }

    public BigDecimal feeRate() {
        return feeRate;
    }

    public BigDecimal fee() {
        return fee;
    }

    public BigDecimal rebateRate() {
        return rebateRate;
    }

    public BigDecimal rebate() {
        return rebate;
    }
}