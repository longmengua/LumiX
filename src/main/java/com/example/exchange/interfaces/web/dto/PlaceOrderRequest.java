package com.example.exchange.interfaces.web.dto;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * PlaceOrderRequest
 * --------------------------
 * 下單 API 的 Request DTO (Data Transfer Object)
 * - 使用 class 實作（非 record）
 * - 搭配 Lombok @Data 自動產生 Getter/Setter/ToString/Equals/HashCode
 * - 使用 Bean Validation 做基本參數檢查
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceOrderRequest {

    /** 使用者 ID */
    @NotNull(message = "uid 不可為空")
    private Long uid;

    /** 交易對，例如 "BTCUSDT" */
    @NotBlank(message = "symbol 不可為空")
    private String symbol;

    /** 下單方向：BUY / SELL */
    @NotNull(message = "side 不可為空")
    private OrderSide side;

    /** 訂單類型：MARKET / LIMIT */
    @NotNull(message = "type 不可為空")
    private OrderType type;

    /**
     * 價格
     * - 限價單 (LIMIT)：必須提供
     * - 市價單 (MARKET)：可為 null，由系統自動補齊
     */
    private BigDecimal price;

    /**
     * 數量
     * - 必填
     * - 最小值 = 0.0001
     */
    @NotNull(message = "qty 不可為空")
    @DecimalMin(value = "0.0001", message = "qty 必須 >= 0.0001")
    private BigDecimal qty;

    /**
     * 槓桿倍數
     * - 必填
     * - 範圍 = 1 ~ 125
     */
    @NotNull(message = "leverage 不可為空")
    @Min(value = 1, message = "leverage 最小為 1")
    @Max(value = 125, message = "leverage 最大為 125")
    private Integer leverage;

    private String clientOrderId;

    private String timeInForce;

    private Boolean reduceOnly;

    /**
     * 保證金模式
     * - 必填
     * - CROSS / ISOLATED
     */
    @NotBlank(message = "marginMode 不可為空")
    private String marginMode;

    public PlaceOrderCommand toPlaceOrderCommand() {
        return new PlaceOrderCommand(
            getUid(),
            getSymbol(),
            getSide(),
            getType(),
            getPrice(),
            getQty(),
            getLeverage(),
            getMarginMode(),
            getClientOrderId(),
            getTimeInForce(),
            Boolean.TRUE.equals(getReduceOnly())
        );
    }
}
