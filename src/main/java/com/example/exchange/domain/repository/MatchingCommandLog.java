/*
 * 檔案用途：Repository contract，定義 matching command log 的 append 與 replay 查詢能力。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-symbol matching command log contract。
 *
 * <p>Production 實作應提供 durable append、per-symbol offset、以及從 checkpoint
 * 後讀取 command 的能力；目前 in-memory adapter 僅供 MVP replay baseline 與測試使用。</p>
 */
public interface MatchingCommandLog {

    MatchingCommandLogEntry append(
            String symbolCode,
            MatchingCommandType type,
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty
    );

    MatchingCommandLogEntry append(
            String symbolCode,
            MatchingCommandType type,
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty,
            String ownerId,
            long ownerEpoch
    );

    MatchingCommandLogEntry appendCancelReplace(
            String symbolCode,
            Order originalOrder,
            Order replacementOrder
    );

    MatchingCommandLogEntry appendCancelReplace(
            String symbolCode,
            Order originalOrder,
            Order replacementOrder,
            String ownerId,
            long ownerEpoch
    );

    List<MatchingCommandLogEntry> listAfter(String symbolCode, long offset);

    List<MatchingCommandLogEntry> listAll(String symbolCode);

    long lastOffset(String symbolCode);
}
