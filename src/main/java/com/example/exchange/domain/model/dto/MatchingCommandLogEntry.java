/*
 * 檔案用途：領域 DTO，承載 matching command log 的單筆可 replay 指令。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


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
@Data
@Builder
@Jacksonized
public class MatchingCommandLogEntry {

    private final String symbolCode;

    private final long offset;

    private final MatchingCommandType type;

    private final Order order;

    private final Order replacementOrder;

    private final BigDecimal newPrice;

    private final BigDecimal newQty;

    private final String ownerId;

    private final long ownerEpoch;

    private final Instant createdAt;
    public MatchingCommandLogEntry(String symbolCode, long offset, MatchingCommandType type, Order order, Order replacementOrder, BigDecimal newPrice, BigDecimal newQty, String ownerId, long ownerEpoch, Instant createdAt) {
        this.symbolCode = symbolCode;
        this.offset = offset;
        this.type = type;
        this.order = order;
        this.replacementOrder = replacementOrder;
        this.newPrice = newPrice;
        this.newQty = newQty;
        this.ownerId = ownerId;
        this.ownerEpoch = ownerEpoch;
        this.createdAt = createdAt;
    }

    public String symbolCode() {
        return symbolCode;
    }

    public long offset() {
        return offset;
    }

    public MatchingCommandType type() {
        return type;
    }

    public Order order() {
        return order;
    }

    public Order replacementOrder() {
        return replacementOrder;
    }

    public BigDecimal newPrice() {
        return newPrice;
    }

    public BigDecimal newQty() {
        return newQty;
    }

    public String ownerId() {
        return ownerId;
    }

    public long ownerEpoch() {
        return ownerEpoch;
    }

    public Instant createdAt() {
        return createdAt;
    }
}