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
 * @param order            command 發生當下的原 order snapshot
 * @param replacementOrder cancel-replace 的 replacement order；其他指令為 null
 * @param newPrice         amend 指令的新價格；其他指令為 null
 * @param newQty           amend 指令的新剩餘數量；其他指令為 null
 * @param ownerId          寫入 command 時的 sequencer owner；in-memory baseline 可為 null
 * @param ownerEpoch       寫入 command 時的 sequencer epoch；未接 fencing 時為 0
 * @param createdAt        command log 建立時間
 */
public record MatchingCommandLogEntry(
        String symbolCode,
        long offset,
        MatchingCommandType type,
        Order order,
        Order replacementOrder,
        BigDecimal newPrice,
        BigDecimal newQty,
        String ownerId,
        long ownerEpoch,
        Instant createdAt
) {
}
