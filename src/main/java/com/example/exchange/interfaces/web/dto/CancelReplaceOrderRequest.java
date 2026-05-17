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

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelReplaceOrderRequest {

    @NotNull(message = "uid 不可為空")
    private Long uid;

    @DecimalMin(value = "0.0001", message = "price 必須 >= 0.0001")
    private BigDecimal price;

    @DecimalMin(value = "0.0001", message = "qty 必須 >= 0.0001")
    private BigDecimal qty;

    private String clientOrderId;

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
