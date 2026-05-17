/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.application.command.CancelReplaceOrderCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST cancel-replace request。
 *
 * <p>price、qty、clientOrderId 至少要提供一個；未提供的欄位會沿用原訂單。</p>
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelReplaceOrderRequest {

    /** 操作使用者，必須與原訂單 uid 相同。 */
    @NotNull(message = "uid 不可為空")
    private Long uid;

    /** replacement price；null 表示沿用原價格。 */
    @DecimalMin(value = "0.0001", message = "price 必須 >= 0.0001")
    private BigDecimal price;

    /** replacement quantity；null 表示沿用原剩餘數量。 */
    @DecimalMin(value = "0.0001", message = "qty 必須 >= 0.0001")
    private BigDecimal qty;

    /** replacement client order id；null 表示沿用原值。 */
    private String clientOrderId;

    /** 將 Web DTO 轉成 use case command，orderId 由 URL path 提供。 */
    public CancelReplaceOrderCommand toCommand(UUID orderId) {
        return new CancelReplaceOrderCommand(
                orderId,
                getUid(),
                getPrice(),
                getQty(),
                getClientOrderId()
        );
    }
}
