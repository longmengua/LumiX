package com.lumix.reservation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 將未消耗 reservation 安全釋放回可用餘額的真實資料庫 runtime。 */
@Service
public class ReservationReleaseService {
    private final JdbcTemplate jdbcTemplate;
    public ReservationReleaseService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate); }

    /**
     * 釋放 reservation 剩餘額。
     *
     * <p>鎖定順序固定為 reservation 再 projection，避免 release 與 capture 競態時產生重複可用餘額。</p>
     */
    @Transactional
    public void release(String reservationId, String requestId, String idempotencyKey, String actorId) {
        Map<String, Object> reservation = lockReservation(reservationId);
        if (!"ACTIVE".equals(reservation.get("status")) && !"PARTIALLY_CONSUMED".equals(reservation.get("status"))) throw new IllegalStateException("reservation cannot be released from current state");
        BigDecimal remaining = (BigDecimal) reservation.get("remaining_amount");
        if (remaining.signum() <= 0) throw new IllegalStateException("reservation has no releasable amount");
        if (jdbcTemplate.update("INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status) VALUES ('RESERVATION_RELEASE', ?, ?, 'IN_PROGRESS') ON CONFLICT (scope, idempotency_key) DO NOTHING", idempotencyKey, requestId) == 0) {
            verifyReplay(reservationId, requestId, idempotencyKey);
            return;
        }
        jdbcTemplate.queryForList("SELECT available_amount FROM balance_projections WHERE account_id = ? AND asset_symbol = ? FOR UPDATE", reservation.get("account_id"), reservation.get("asset_symbol"));
        jdbcTemplate.update("UPDATE reservations SET remaining_amount = 0, released_amount = released_amount + ?, status = 'RELEASED', updated_at = CURRENT_TIMESTAMP WHERE reservation_id = ?", remaining, reservationId);
        jdbcTemplate.update("UPDATE balance_projections SET available_amount = available_amount + ?, locked_amount = locked_amount - ?, projection_version = projection_version + 1, projected_at = CURRENT_TIMESTAMP, reconciled_at = NULL WHERE account_id = ? AND asset_symbol = ?", remaining, remaining, reservation.get("account_id"), reservation.get("asset_symbol"));
        jdbcTemplate.update("INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome) VALUES ('SYSTEM', ?, 'RESERVATION_RELEASE', 'RESERVATION', ?, ?, 'SUCCESS')", actorId, reservationId, requestId);
        jdbcTemplate.update("UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'RESERVATION', resource_id = ?, updated_at = CURRENT_TIMESTAMP WHERE scope = 'RESERVATION_RELEASE' AND idempotency_key = ?", reservationId, idempotencyKey);
    }
    private Map<String, Object> lockReservation(String reservationId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT reservation_id, account_id, asset_symbol, status, remaining_amount FROM reservations WHERE reservation_id = ? FOR UPDATE", reservationId);
        if (rows.size() != 1) throw new IllegalArgumentException("reservation does not exist");
        return rows.getFirst();
    }
    private void verifyReplay(String reservationId, String requestId, String idempotencyKey) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT request_id, status, resource_id FROM idempotency_keys WHERE scope = 'RESERVATION_RELEASE' AND idempotency_key = ?", idempotencyKey);
        if (rows.size() != 1 || !requestId.equals(rows.getFirst().get("request_id"))
                || !"COMPLETED".equals(rows.getFirst().get("status"))
                || !reservationId.equals(rows.getFirst().get("resource_id"))) {
            throw new IllegalStateException("reservation release idempotency conflict is not safely replayable");
        }
    }
}
