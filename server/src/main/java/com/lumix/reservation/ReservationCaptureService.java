package com.lumix.reservation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 將已 hold 的資產標記為 consumed；capture 本身不取代必要的 ledger posting。 */
@Service
public class ReservationCaptureService {
    private final JdbcTemplate jdbcTemplate;
    public ReservationCaptureService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate); }

    /** 消耗全部 remaining amount，並自 locked projection 移除；沒有 ledger journal 時不得視為資產移轉完成。 */
    @Transactional
    public void capture(String reservationId, String requestId, String idempotencyKey, String actorId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT account_id, asset_symbol, status, remaining_amount FROM reservations WHERE reservation_id = ? FOR UPDATE", reservationId);
        if (rows.size() != 1 || (!"ACTIVE".equals(rows.getFirst().get("status")) && !"PARTIALLY_CONSUMED".equals(rows.getFirst().get("status")))) throw new IllegalStateException("reservation cannot be captured");
        Map<String, Object> reservation = rows.getFirst();
        BigDecimal amount = (BigDecimal) reservation.get("remaining_amount");
        if (amount.signum() <= 0) throw new IllegalStateException("reservation has no capturable amount");
        if (jdbcTemplate.update("INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status) VALUES ('RESERVATION_CAPTURE', ?, ?, 'IN_PROGRESS') ON CONFLICT (scope, idempotency_key) DO NOTHING", idempotencyKey, requestId) == 0) throw new IllegalStateException("reservation capture key already exists");
        jdbcTemplate.queryForList("SELECT locked_amount FROM balance_projections WHERE account_id = ? AND asset_symbol = ? FOR UPDATE", reservation.get("account_id"), reservation.get("asset_symbol"));
        jdbcTemplate.update("UPDATE reservations SET remaining_amount = 0, consumed_amount = consumed_amount + ?, status = 'CONSUMED', updated_at = CURRENT_TIMESTAMP WHERE reservation_id = ?", amount, reservationId);
        // capture 單獨不能破壞 total = available + locked；同一 transaction 後續的 ledger debit 會再把 total 與 available 落到最終值。
        jdbcTemplate.update("UPDATE balance_projections SET available_amount = available_amount + ?, locked_amount = locked_amount - ?, projection_version = projection_version + 1, projected_at = CURRENT_TIMESTAMP, reconciled_at = NULL WHERE account_id = ? AND asset_symbol = ?", amount, amount, reservation.get("account_id"), reservation.get("asset_symbol"));
        jdbcTemplate.update("INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome) VALUES ('SYSTEM', ?, 'RESERVATION_CAPTURE', 'RESERVATION', ?, ?, 'SUCCESS')", actorId, reservationId, requestId);
        jdbcTemplate.update("UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'RESERVATION', resource_id = ? WHERE scope = 'RESERVATION_CAPTURE' AND idempotency_key = ?", reservationId, idempotencyKey);
    }
}
