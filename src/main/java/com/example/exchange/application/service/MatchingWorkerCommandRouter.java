/*
 * 檔案用途：應用服務，為 production matching worker 提供 lease-fenced command/event append 入口。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.MatchingEventLogEntry;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.repository.MatchingCommandLog;
import com.example.exchange.domain.repository.MatchingEventLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Production matching worker command router.
 *
 * <p>此服務是 live worker 寫入 durable matching command/event log 的防線。任何 append
 * 前都必須驗證 caller 仍持有該 symbol 的 sequencer lease，並把 owner/epoch 寫進 log
 * 供 replay、fencing audit 與事故排查使用。</p>
 */
@Service
@RequiredArgsConstructor
public class MatchingWorkerCommandRouter {

    private final MatchingSequencerLeaseService leaseService;
    private final MatchingCommandLog commandLog;
    private final MatchingEventLog eventLog;

    public MatchingCommandLogEntry appendCommand(
            String symbolCode,
            MatchingCommandType type,
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty,
            String ownerId,
            long ownerEpoch
    ) {
        leaseService.requireWritable(symbolCode, ownerId, ownerEpoch);
        return commandLog.append(symbolCode, type, order, newPrice, newQty, ownerId, ownerEpoch);
    }

    public MatchingCommandLogEntry appendCancelReplace(
            String symbolCode,
            Order originalOrder,
            Order replacementOrder,
            String ownerId,
            long ownerEpoch
    ) {
        leaseService.requireWritable(symbolCode, ownerId, ownerEpoch);
        return commandLog.appendCancelReplace(symbolCode, originalOrder, replacementOrder, ownerId, ownerEpoch);
    }

    public MatchingEventLogEntry appendEvent(
            String symbolCode,
            long commandOffset,
            TradeExecuted trade,
            String ownerId,
            long ownerEpoch
    ) {
        leaseService.requireWritable(symbolCode, ownerId, ownerEpoch);
        return eventLog.append(symbolCode, commandOffset, trade, ownerId, ownerEpoch);
    }
}
