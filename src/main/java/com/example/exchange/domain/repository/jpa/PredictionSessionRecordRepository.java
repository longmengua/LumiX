package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Session signer repository。
 *
 * 正式用途：
 * 1. session init
 * 2. session confirm
 * 3. session revoke
 * 4. session expire
 * 5. session query
 */
public interface PredictionSessionRecordRepository
        extends JpaRepository<PredictionSessionRecord, Long> {

    /**
     * 查 sessionId。
     */
    Optional<PredictionSessionRecord> findBySessionId(
            String sessionId
    );

    /**
     * 查 ACTIVE session。
     *
     * placeOrder 前使用。
     */
    Optional<PredictionSessionRecord> findBySessionIdAndStatus(
            String sessionId,
            String status
    );

    /**
     * 查使用者所有 session。
     *
     * 用於：
     * 1. session list
     * 2. revoke all
     * 3. activity page
     */
    List<PredictionSessionRecord> findByUserAddressOrderByIdDesc(
            String userAddress
    );

    /**
     * 查指定狀態 sessions。
     *
     * TODO:
     * scheduler 清理 expired session 時可用。
     */
    List<PredictionSessionRecord> findByStatus(
            String status
    );

    /**
     * 查過期 ACTIVE sessions。
     *
     * expiresAt:
     * unix epoch millis。
     */
    List<PredictionSessionRecord>
    findByStatusAndExpiresAtLessThan(
            String status,
            Long expiresAt
    );

    /**
     * 查某 wallet 是否已有 ACTIVE session。
     *
     * TODO:
     * 如果後續想限制：
     * 一個 wallet 只能一個 ACTIVE session，
     * 可使用這個。
     */
    List<PredictionSessionRecord>
    findByUserAddressAndStatus(
            String userAddress,
            String status
    );
}