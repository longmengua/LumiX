/*
 * 檔案用途：Repository contract，定義 matching event log 的 append 與 checkpoint 查詢能力。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingEventLogEntry;

import java.util.List;

/**
 * Per-symbol matching event log contract。
 *
 * <p>Production 實作應將撮合事件寫入 durable event log，並能依 offset
 * 做 recovery、audit 與 replay validation；目前 in-memory adapter 是 baseline。</p>
 */
public interface MatchingEventLog {

    MatchingEventLogEntry append(String symbolCode, long commandOffset, TradeExecuted trade);

    MatchingEventLogEntry append(
            String symbolCode,
            long commandOffset,
            TradeExecuted trade,
            String ownerId,
            long ownerEpoch
    );

    List<MatchingEventLogEntry> listAfter(String symbolCode, long offset);

    List<MatchingEventLogEntry> listAll(String symbolCode);

    long lastOffset(String symbolCode);
}
