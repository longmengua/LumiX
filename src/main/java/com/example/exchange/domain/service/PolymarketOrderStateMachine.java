/*
 * 檔案用途：Polymarket local/CLOB order 狀態轉換 guard。
 */
package com.example.exchange.domain.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PolymarketOrderStateMachine {

    private static final Set<String> ACTIVE_REMOTE_STATUSES =
            Set.of("LIVE", "MATCHED", "ORDER_STATUS_LIVE", "ORDER_STATUS_MATCHED");

    private static final Set<String> TERMINAL_STATUSES =
            Set.of(
                    "CANCELED",
                    "CANCELLED",
                    "ORDER_STATUS_CANCELED",
                    "ORDER_STATUS_CANCELLED",
                    "FILLED",
                    "ORDER_STATUS_FILLED",
                    "SETTLED",
                    "ORDER_STATUS_SETTLED",
                    "FAILED",
                    "REJECTED"
            );

    private static final Set<String> TRADE_MATCH_STATUSES =
            Set.of("MATCHED", "TRADE_STATUS_MATCHED", "FILL", "FILLED", "ORDER_STATUS_MATCHED");

    private static final Set<String> SETTLEMENT_STATUSES =
            Set.of("SETTLED", "ORDER_STATUS_SETTLED", "SETTLEMENT_CONFIRMED", "REDEEMED");

    private static final List<TransitionRule> TRANSITION_MATRIX =
            List.of(
                    new TransitionRule(LifecycleStage.LOCAL, null, "CREATE_LOCAL", "CREATED", false, false,
                            "Local order row is opened before sending an effectful CLOB command."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "CREATED", "ACCEPTED", "ACCEPTED", false, false,
                            "CLOB accepted the signed order request."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "ACCEPTED", "ORDER_STATUS_LIVE", "ORDER_STATUS_LIVE", false, false,
                            "Remote order is live and can still be matched or canceled."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "ORDER_STATUS_LIVE", "ORDER_STATUS_MATCHED", "ORDER_STATUS_MATCHED", false, false,
                            "Remote order has matched size but is not settled yet."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "ORDER_STATUS_MATCHED", "ORDER_STATUS_FILLED", "ORDER_STATUS_FILLED", true, false,
                            "Remote order is fully filled."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "CANCEL_OUTCOME_UNCERTAIN", "ORDER_STATUS_CANCELED", "ORDER_STATUS_CANCELED", true, false,
                            "Reconcile resolves an uncertain cancel outcome with a terminal remote status."),
                    new TransitionRule(LifecycleStage.TRADE, "ORDER_STATUS_LIVE", "MATCHED", "ORDER_STATUS_MATCHED", false, false,
                            "User-channel trade event confirms order execution before settlement."),
                    new TransitionRule(LifecycleStage.SETTLEMENT, "ORDER_STATUS_MATCHED", "SETTLED", "ORDER_STATUS_SETTLED", true, false,
                            "Settlement/redeem event closes the local lifecycle."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "ORDER_STATUS_FILLED", "ORDER_STATUS_LIVE", "ORDER_STATUS_FILLED", true, true,
                            "Stale active CLOB payload cannot downgrade a local terminal fill."),
                    new TransitionRule(LifecycleStage.CLOB_ORDER, "ORDER_STATUS_SETTLED", "ORDER_STATUS_MATCHED", "ORDER_STATUS_SETTLED", true, true,
                            "Stale active CLOB payload cannot downgrade local settlement.")
            );

    public List<TransitionRule> transitionMatrix() {
        return TRANSITION_MATRIX;
    }

    public String resolveRemoteStatus(String currentStatus, String remoteStatus) {
        String normalizedRemote =
                normalize(remoteStatus);
        if (normalizedRemote == null) {
            return currentStatus;
        }

        String normalizedCurrent =
                normalize(currentStatus);
        if (normalizedCurrent != null
                && TERMINAL_STATUSES.contains(normalizedCurrent)
                && ACTIVE_REMOTE_STATUSES.contains(normalizedRemote)) {
            return currentStatus;
        }

        return remoteStatus;
    }

    public LifecycleTransition resolveUserEventStatus(
            String currentOrderStatus,
            String currentTradeStatus,
            String eventType,
            String eventStatus
    ) {
        String normalizedEventType =
                normalize(eventType);
        String normalizedEventStatus =
                normalize(eventStatus);
        if (normalizedEventStatus == null) {
            return new LifecycleTransition(currentOrderStatus, currentTradeStatus, LifecycleStage.LOCAL, false);
        }

        if ("TRADE".equals(normalizedEventType)) {
            String nextOrderStatus =
                    shouldPromoteTradeMatch(currentOrderStatus, normalizedEventStatus)
                            ? "ORDER_STATUS_MATCHED"
                            : currentOrderStatus;
            return new LifecycleTransition(
                    nextOrderStatus,
                    eventStatus,
                    LifecycleStage.TRADE,
                    !equalsNormalized(currentOrderStatus, nextOrderStatus)
                            || !equalsNormalized(currentTradeStatus, eventStatus)
            );
        }

        if ("SETTLEMENT".equals(normalizedEventType)
                || "SETTLED".equals(normalizedEventType)
                || SETTLEMENT_STATUSES.contains(normalizedEventStatus)) {
            String nextOrderStatus =
                    resolveSettlementStatus(currentOrderStatus, normalizedEventStatus);
            return new LifecycleTransition(
                    nextOrderStatus,
                    currentTradeStatus,
                    LifecycleStage.SETTLEMENT,
                    !equalsNormalized(currentOrderStatus, nextOrderStatus)
            );
        }

        String nextOrderStatus =
                resolveRemoteStatus(currentOrderStatus, eventStatus);
        return new LifecycleTransition(
                nextOrderStatus,
                currentTradeStatus,
                LifecycleStage.CLOB_ORDER,
                !equalsNormalized(currentOrderStatus, nextOrderStatus)
        );
    }

    public boolean shouldApplyRemoteMatchedSize(String currentStatus, String remoteStatus) {
        String normalizedCurrent =
                normalize(currentStatus);
        String normalizedRemote =
                normalize(remoteStatus);
        return normalizedCurrent == null
                || normalizedRemote == null
                || !TERMINAL_STATUSES.contains(normalizedCurrent)
                || !ACTIVE_REMOTE_STATUSES.contains(normalizedRemote);
    }

    private String resolveSettlementStatus(String currentStatus, String eventStatus) {
        String normalizedCurrent =
                normalize(currentStatus);
        if (normalizedCurrent != null && TERMINAL_STATUSES.contains(normalizedCurrent)) {
            return currentStatus;
        }
        return SETTLEMENT_STATUSES.contains(normalize(eventStatus)) ? "ORDER_STATUS_SETTLED" : currentStatus;
    }

    private boolean shouldPromoteTradeMatch(String currentStatus, String eventStatus) {
        String normalizedCurrent =
                normalize(currentStatus);
        if (normalizedCurrent != null && TERMINAL_STATUSES.contains(normalizedCurrent)) {
            return false;
        }
        return TRADE_MATCH_STATUSES.contains(normalize(eventStatus));
    }

    private static boolean equalsNormalized(String left, String right) {
        String normalizedLeft =
                normalize(left);
        String normalizedRight =
                normalize(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return normalizedLeft == normalizedRight;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    private static String normalize(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    public enum LifecycleStage {
        LOCAL,
        CLOB_ORDER,
        TRADE,
        SETTLEMENT
    }

    public record TransitionRule(
            LifecycleStage stage,
            String currentStatus,
            String externalStatus,
            String nextStatus,
            boolean terminal,
            boolean protectsTerminalDowngrade,
            String note
    ) {
    }

    public record LifecycleTransition(
            String orderStatus,
            String tradeStatus,
            LifecycleStage stage,
            boolean changed
    ) {
    }
}
