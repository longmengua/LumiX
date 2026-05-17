/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.application.command.AmendOrderCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST 改單 request。
 *
 * <p>price、qty、clientOrderId 至少要提供一個；實際完整驗證在 AmendOrderUseCase。</p>
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmendOrderRequest {

    /** 操作使用者，必須與原訂單 uid 相同。 */
    @NotNull(message = "uid 不可為空")
    private Long uid;

    /** 新價格；null 表示不改價格。 */
    @DecimalMin(value = "0.0001", message = "price 必須 >= 0.0001")
    private BigDecimal price;

    /** 新剩餘數量；null 表示不改數量。 */
    @DecimalMin(value = "0.0001", message = "qty 必須 >= 0.0001")
    private BigDecimal qty;

    /** 新 client order id；null 表示不改。 */
    private String clientOrderId;

    /** 將 Web DTO 轉成 use case command，orderId 由 URL path 提供。 */
    public AmendOrderCommand toCommand(UUID orderId) {
        return new AmendOrderCommand(
                orderId,
                getUid(),
                getPrice(),
                getQty(),
                getClientOrderId()
        );
    }
}
