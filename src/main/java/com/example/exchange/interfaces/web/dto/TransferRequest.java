package com.example.exchange.interfaces.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * TransferRequest
 * --------------------------
 * 劃轉 API 的 Request DTO
 * - 使用 Java record 來定義不可變物件
 * - 搭配 Lombok 與 Jackson 註解，簡化建構與序列化
 */
@Builder                      // Lombok: 產生建構器 (Builder 模式)，方便建立物件
@Jacksonized                  // Lombok + Jackson: 讓 Builder 支援 JSON 反序列化
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略 JSON 中多餘欄位，避免序列化錯誤
public record TransferRequest(

        /** 使用者 ID */
        @NotNull(message = "uid 不可為空")
        Long uid,

        /** 交易對，例如 "BTCUSDT" */
        @NotBlank(message = "symbol 不可為空")
        String symbol,

        /** 是否劃轉到逐倉
         * - true  = 劃轉到逐倉 (Isolated)
         * - false = 劃轉回全倉 (Cross)
         */
        boolean toIsolated,

        /** 劃轉金額
         * - 必填
         * - 最小值 = 0.0001
         */
        @DecimalMin(value = "0.0001", message = "amount 必須 >= 0.0001")
        BigDecimal amount
) {}
