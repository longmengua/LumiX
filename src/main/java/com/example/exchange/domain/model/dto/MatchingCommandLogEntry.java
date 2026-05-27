/*
 * 檔案用途：領域 DTO，承載 matching command log 的單筆可 replay 指令。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 單一 symbol 的撮合 command log entry。
 *
 * @param symbolCode normalized symbol code
 * @param offset     per-symbol command offset
 * @param type       command type
 * @param order      command 發生當下的 order snapshot
 * @param newPrice   amend 指令的新價格；其他指令為 null
 * @param newQty     amend 指令的新剩餘數量；其他指令為 null
 * @param createdAt  command log 建立時間
 */
public record MatchingCommandLogEntry(
        String symbolCode,
        long offset,
        MatchingCommandType type,
        Order order,
        BigDecimal newPrice,
        BigDecimal newQty,
        Instant createdAt
) {
}
