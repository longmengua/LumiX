/*
 * 檔案用途：應用服務，提供 production matching worker 的 lease-fenced command execution 入口。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Production matching worker execution service。
 *
 * <p>此服務固定 live worker 的順序：先通過 sequencer lease guard 並 append durable command
 * log，再將已落 log 的 command 交給 engine 執行。這避免 worker execution 繞過 owner/epoch
 * fencing，也避免 engine 在 worker path 內重複 append command。</p>
 */
@Service
@RequiredArgsConstructor
public class MatchingWorkerExecutionService {

    private final MatchingWorkerCommandRouter commandRouter;
    private final MatchingEngine matchingEngine;

    public MatchingResult submit(Order order, String ownerId, long ownerEpoch) {
        if (order == null || order.getSymbol() == null) {
            throw new IllegalArgumentException("order and symbol must not be null");
        }
        MatchingCommandLogEntry command = commandRouter.appendCommand(
                order.getSymbol().code(),
                MatchingCommandType.SUBMIT,
                order,
                null,
                null,
                ownerId,
                ownerEpoch
        );
        return matchingEngine.applyLoggedCommand(command);
    }

    public MatchingResult cancel(Order order, String ownerId, long ownerEpoch) {
        if (order == null || order.getSymbol() == null) {
            throw new IllegalArgumentException("order and symbol must not be null");
        }
        MatchingCommandLogEntry command = commandRouter.appendCommand(
                order.getSymbol().code(),
                MatchingCommandType.CANCEL,
                order,
                null,
                null,
                ownerId,
                ownerEpoch
        );
        return matchingEngine.applyLoggedCommand(command);
    }

    public MatchingResult amend(
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty,
            String ownerId,
            long ownerEpoch
    ) {
        if (order == null || order.getSymbol() == null) {
            throw new IllegalArgumentException("order and symbol must not be null");
        }
        MatchingCommandLogEntry command = commandRouter.appendCommand(
                order.getSymbol().code(),
                MatchingCommandType.AMEND,
                order,
                newPrice,
                newQty,
                ownerId,
                ownerEpoch
        );
        return matchingEngine.applyLoggedCommand(command);
    }

    public MatchingResult cancelReplace(
            Order originalOrder,
            Order replacementOrder,
            String ownerId,
            long ownerEpoch
    ) {
        if (originalOrder == null || originalOrder.getSymbol() == null || replacementOrder == null) {
            throw new IllegalArgumentException("originalOrder, replacementOrder and symbol must not be null");
        }
        MatchingCommandLogEntry command = commandRouter.appendCancelReplace(
                originalOrder.getSymbol().code(),
                originalOrder,
                replacementOrder,
                ownerId,
                ownerEpoch
        );
        return matchingEngine.applyLoggedCommand(command);
    }
}
