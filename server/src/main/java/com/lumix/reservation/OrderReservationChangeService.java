package com.lumix.reservation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 現貨與合約訂單共用的部分 reservation 扣用／釋放，不暴露 HTTP endpoint。
 *
 * <p>HUMAN_REVIEW_REQUIRED：此類別會改變可用／鎖定投影。呼叫端必須在同一個 primary transaction
 * 完成成交 ledger、order/trade 更新或撤單；CAPTURE 本身不表示已結算。強制加入既有 transaction，
 * 防止此內部步驟自行 commit 後，外層結算失敗卻留下已釋放資金。</p>
 *
 * <p>鎖定順序為 order → idempotency claim → reservation → projection。跨多筆訂單／帳戶的外層
 * 撮合服務仍需先按固定順序取得整組資源鎖，不能假設此單筆方法能替代整場撮合的鎖定協議。</p>
 */
@Service
public class OrderReservationChangeService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public OrderReservationChangeService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.json = Objects.requireNonNull(json);
    }

    /**
     * 只變更綁定該 owner/account/order 的 ORDER_HOLD；相同鍵不同命令失敗，重播沿用原始快照。
     * requestId 是本次追蹤資訊，不納入業務 fingerprint，允許網路重試換 requestId。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OrderReservationChangeResult change(OrderReservationChange command) {
        Objects.requireNonNull(command, "command is required");
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("order reservation change requires an outer write transaction");
        }
        String orderStatus = lockOwnedOrder(command);
        String scope = command.action() == OrderReservationChange.Action.CAPTURE
                ? "RESERVATION_CAPTURE" : "RESERVATION_RELEASE";
        // 使用獨立 key 前綴與 owner scope，不與既有整筆提款／劃轉命令碰撞，也不要求歷史 key adapter。
        String key = "order:" + digest(encode(List.of(command.ownerUserId(), command.idempotencyKey())));
        String fingerprint = fingerprint(command);
        if (jdbc.update("INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status) "
                        + "VALUES (?, ?, ?, 'IN_PROGRESS') ON CONFLICT (scope, idempotency_key) DO NOTHING",
                scope, key, command.requestId()) == 0) {
            return replay(scope, key, fingerprint, command.reservationId());
        }

        if (command.action() == OrderReservationChange.Action.CAPTURE
                && !List.of("ACCEPTED", "OPEN", "PARTIALLY_FILLED").contains(orderStatus)) {
            throw new IllegalStateException("order is not executable");
        }

        Map<String, Object> row = lockReservation(command);
        BigDecimal remaining = (BigDecimal) row.get("remaining_amount");
        BigDecimal consumed = (BigDecimal) row.get("consumed_amount");
        BigDecimal released = (BigDecimal) row.get("released_amount");
        if ((!"ACTIVE".equals(row.get("status")) && !"PARTIALLY_CONSUMED".equals(row.get("status")))
                || remaining.compareTo(command.amount()) < 0) {
            throw new IllegalStateException("order reservation amount is unavailable");
        }
        if (command.amount().scale() > ((Number) row.get("precision_scale")).intValue()) {
            throw new IllegalArgumentException("amount exceeds asset precision");
        }

        lockProjection(command);
        remaining = remaining.subtract(command.amount());
        if (command.action() == OrderReservationChange.Action.CAPTURE) consumed = consumed.add(command.amount());
        else released = released.add(command.amount());
        String state = remaining.signum() == 0
                ? (command.action() == OrderReservationChange.Action.CAPTURE ? "CONSUMED" : "RELEASED")
                : (consumed.signum() > 0 ? "PARTIALLY_CONSUMED" : "ACTIVE");
        requireOne(jdbc.update("UPDATE reservations SET remaining_amount = ?, consumed_amount = ?, "
                        + "released_amount = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE reservation_id = ?",
                remaining, consumed, released, state, command.reservationId()));
        // CAPTURE 與 RELEASE 都只解除此次金額的 lock；CAPTURE 的實際 debit 留在同交易的 ledger posting。
        requireOne(jdbc.update("UPDATE balance_projections SET available_amount = available_amount + ?, "
                        + "locked_amount = locked_amount - ?, projection_version = projection_version + 1, "
                        + "projected_at = CURRENT_TIMESTAMP, reconciled_at = NULL "
                        + "WHERE account_id = ? AND asset_symbol = ? AND locked_amount >= ?",
                command.amount(), command.amount(), command.accountId(), command.assetSymbol(), command.amount()));

        OrderReservationChangeResult result = new OrderReservationChangeResult(remaining, consumed, released, state, false);
        appendEvidence(command, result);
        requireOne(jdbc.update("UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'RESERVATION', "
                        + "resource_id = ?, response_summary = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE scope = ? AND idempotency_key = ? AND status = 'IN_PROGRESS'",
                command.reservationId(), encode(new SavedChange(fingerprint, result)), scope, key));
        return result;
    }

    private String lockOwnedOrder(OrderReservationChange command) {
        List<String> orders = jdbc.query("SELECT o.status FROM orders o "
                        + "JOIN accounts a ON a.account_id = o.account_id AND a.user_id = o.user_id "
                        + "WHERE o.order_id = ? AND o.user_id = ? AND o.account_id = ? "
                        + "AND a.account_category = 'USER' AND a.account_type IN ('SPOT', 'FUTURES') FOR UPDATE OF o",
                (rs, n) -> rs.getString(1), command.orderId(), command.ownerUserId(), command.accountId());
        if (orders.size() != 1) throw new IllegalArgumentException("owned trading order is required");
        return orders.getFirst();
    }

    private Map<String, Object> lockReservation(OrderReservationChange command) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.status, r.remaining_amount, r.consumed_amount, "
                        + "r.released_amount, a.precision_scale FROM reservations r "
                        + "JOIN assets a ON a.asset_symbol = r.asset_symbol "
                        + "WHERE r.reservation_id = ? AND r.account_id = ? AND r.asset_symbol = ? "
                        + "AND r.business_reference_type = 'ORDER' AND r.business_reference_id = ? "
                        + "AND r.reservation_type = 'ORDER_HOLD' FOR UPDATE OF r",
                command.reservationId(), command.accountId(), command.assetSymbol(), command.orderId());
        if (rows.size() != 1) throw new IllegalArgumentException("matching order reservation is required");
        return rows.getFirst();
    }

    private void lockProjection(OrderReservationChange command) {
        List<BigDecimal> rows = jdbc.query("SELECT locked_amount FROM balance_projections "
                        + "WHERE account_id = ? AND asset_symbol = ? FOR UPDATE",
                (rs, n) -> rs.getBigDecimal(1), command.accountId(), command.assetSymbol());
        if (rows.size() != 1 || rows.getFirst().compareTo(command.amount()) < 0) {
            throw new IllegalStateException("order reservation projection is inconsistent");
        }
    }

    private OrderReservationChangeResult replay(String scope, String key, String fingerprint, String reservationId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT status, resource_type, resource_id, response_summary "
                + "FROM idempotency_keys WHERE scope = ? AND idempotency_key = ?", scope, key);
        if (rows.size() != 1 || !"COMPLETED".equals(rows.getFirst().get("status"))
                || !"RESERVATION".equals(rows.getFirst().get("resource_type"))
                || !reservationId.equals(rows.getFirst().get("resource_id"))) {
            throw new IllegalStateException("order reservation key is not safely replayable");
        }
        try {
            SavedChange saved = json.readValue((String) rows.getFirst().get("response_summary"), SavedChange.class);
            if (saved == null || !fingerprint.equals(saved.fingerprint()) || saved.result() == null) {
                throw new IllegalArgumentException("order reservation key conflicts with command");
            }
            return saved.result().asReplay();
        } catch (JsonProcessingException | NullPointerException exception) {
            throw new IllegalStateException("order reservation evidence is invalid", exception);
        }
    }

    private void appendEvidence(OrderReservationChange command, OrderReservationChangeResult result) {
        String event = "ORDER_RESERVATION_" + command.action().name();
        requireOne(jdbc.update("INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, "
                        + "request_id, outcome, reason) VALUES ('SYSTEM', 'trading-runtime', ?, 'RESERVATION', ?, ?, 'SUCCESS', ?)",
                event, command.reservationId(), command.requestId(), encode(List.of(command.orderId(), command.amount().toPlainString()))));
        requireOne(jdbc.update("INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, status, request_id) "
                        + "VALUES ('RESERVATION', ?, ?, ?, 'READY', ?)",
                command.reservationId(), event, encode(new ChangeEvidence(command.orderId(), command.action().name(),
                        command.amount(), result)), command.requestId()));
    }

    private String fingerprint(OrderReservationChange c) {
        // JSON array 消除任意分隔符歧義；金額已 canonicalize，10.0 與 10.00 視為同一業務命令。
        return digest(encode(List.of(c.action().name(), c.ownerUserId(), c.accountId(), c.orderId(),
                c.reservationId(), c.assetSymbol(), c.amount().toPlainString())));
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("cannot encode order reservation evidence", exception); }
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }

    private static void requireOne(int affected) {
        if (affected != 1) throw new IllegalStateException("order reservation write was not applied exactly once");
    }

    private record SavedChange(String fingerprint, OrderReservationChangeResult result) { }
    private record ChangeEvidence(String orderId, String action, BigDecimal amount, OrderReservationChangeResult result) { }
}
