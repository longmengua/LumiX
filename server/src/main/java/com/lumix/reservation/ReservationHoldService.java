package com.lumix.reservation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 真實資料庫 reservation HOLD 邊界。
 *
 * <p>鎖定 projection row 是為了讓同一 account/asset 的 concurrent hold 串行化；金額不足時整筆
 * transaction 回滾，絕不建立半筆 reservation 或直接修改 ledger。</p>
 */
@Service
public class ReservationHoldService {
    private final JdbcTemplate jdbcTemplate;
    public ReservationHoldService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate); }

    /** 建立一次性 HOLD；相同 idempotency key 只回放既有 reservation。 */
    @Transactional
    public String hold(ReservationHoldCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (jdbcTemplate.update("INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status) VALUES ('RESERVATION_HOLD', ?, ?, 'IN_PROGRESS') ON CONFLICT (scope, idempotency_key) DO NOTHING", command.idempotencyKey(), command.requestId()) == 0) {
            return replay(command);
        }
        Map<String, Object> balance = lockedBalance(command);
        BigDecimal available = (BigDecimal) balance.get("available_amount");
        if (available.compareTo(command.amount()) < 0) throw new IllegalStateException("insufficient available balance for reservation hold");
        jdbcTemplate.update("INSERT INTO reservations (reservation_id, account_id, asset_symbol, business_reference_type, business_reference_id, reservation_type, status, original_amount, remaining_amount, request_id) VALUES (?, ?, ?, 'ADJUSTMENT', ?, 'ADMIN_HOLD', 'ACTIVE', ?, ?, ?)", command.reservationId(), command.accountId(), command.assetSymbol(), command.businessReferenceId(), command.amount(), command.amount(), command.requestId());
        jdbcTemplate.update("UPDATE balance_projections SET available_amount = available_amount - ?, locked_amount = locked_amount + ?, projection_version = projection_version + 1, projected_at = CURRENT_TIMESTAMP, reconciled_at = NULL WHERE account_id = ? AND asset_symbol = ?", command.amount(), command.amount(), command.accountId(), command.assetSymbol());
        jdbcTemplate.update("INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome) VALUES ('SYSTEM', ?, 'RESERVATION_HOLD', 'RESERVATION', ?, ?, 'SUCCESS')", command.actorId(), command.reservationId(), command.requestId());
        jdbcTemplate.update("UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'RESERVATION', resource_id = ?, updated_at = CURRENT_TIMESTAMP WHERE scope = 'RESERVATION_HOLD' AND idempotency_key = ? AND status = 'IN_PROGRESS'", command.reservationId(), command.idempotencyKey());
        return command.reservationId();
    }
    private Map<String, Object> lockedBalance(ReservationHoldCommand command) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT available_amount FROM balance_projections WHERE account_id = ? AND asset_symbol = ? FOR UPDATE", command.accountId(), command.assetSymbol());
        if (rows.size() != 1) throw new IllegalStateException("balance projection is required before reservation hold");
        return rows.getFirst();
    }
    private String replay(ReservationHoldCommand command) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT request_id, status, resource_id FROM idempotency_keys WHERE scope = 'RESERVATION_HOLD' AND idempotency_key = ?", command.idempotencyKey());
        if (rows.size() != 1 || !command.requestId().equals(rows.getFirst().get("request_id")) || !"COMPLETED".equals(rows.getFirst().get("status"))) throw new IllegalStateException("reservation idempotency conflict is not safely replayable");
        return rows.getFirst().get("resource_id").toString();
    }
}
