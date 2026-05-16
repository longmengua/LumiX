package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Polymarket session signer record。
 *
 * 正式架構：
 *
 * Deposit Wallet：
 * - 使用者 MetaMask
 * - 真正持有資產
 * - approve exchange
 *
 * Session Signer：
 * - 後端生成 ephemeral wallet
 * - 只負責簽 CLOB order
 * - 不持有資產
 * - 可 revoke / expire
 *
 * 流程：
 * 1. init session
 * 2. eth_signTypedData_v4
 * 3. confirm session
 * 4. ACTIVE
 * 5. place order
 */
@Entity
@Table(
        name = "polymarket_session",
        indexes = {
                @Index(
                        name = "idx_session_id",
                        columnList = "session_id",
                        unique = true
                ),
                @Index(
                        name = "idx_user_address",
                        columnList = "user_address"
                ),
                @Index(
                        name = "idx_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_expires_at",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionSessionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session UUID。
     */
    @Column(
            name = "session_id",
            nullable = false,
            length = 64,
            unique = true
    )
    private String sessionId;

    /**
     * Deposit wallet / MetaMask owner。
     *
     * 真正持有資產的 wallet。
     */
    @Column(
            name = "user_address",
            nullable = false,
            length = 64
    )
    private String userAddress;

    /**
     * 後端生成的 session signer address。
     *
     * 只負責：
     * sign CLOB order。
     */
    @Column(
            name = "session_signer_address",
            nullable = false,
            length = 64
    )
    private String sessionSignerAddress;

    /**
     * Session signer private key。
     *
     * TODO:
     * 正式環境必須：
     * 1. AES encrypt
     * 2. KMS
     * 3. Vault
     * 4. rotation
     *
     * 不要明文保存。
     */
    @Column(
            name = "session_private_key",
            nullable = false,
            length = 512
    )
    private String sessionPrivateKey;

    /**
     * EIP712 typedData json。
     *
     * 前端：
     * eth_signTypedData_v4
     *
     * 必須使用 LONGTEXT，
     * 因為 typedData JSON 可能超過 MySQL TEXT/VARCHAR 長度。
     *
     * TODO:
     * 後續如果 typedData 太大，
     * 可只保存 typedData hash。
     */
    @Lob
    @Column(
            name = "typed_data",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String typedData;

    /**
     * EIP712 signature。
     *
     * MetaMask:
     * eth_signTypedData_v4
     */
    @Lob
    @Column(
            name = "signature",
            columnDefinition = "LONGTEXT"
    )
    private String signature;

    /**
     * Session 狀態。
     *
     * PENDING：
     * 尚未 confirm。
     *
     * ACTIVE：
     * 可下單。
     *
     * EXPIRED：
     * 已過期。
     *
     * REVOKED：
     * 使用者手動撤銷。
     */
    @Column(
            name = "status",
            nullable = false,
            length = 32
    )
    private String status;

    /**
     * Session issue timestamp。
     *
     * Unix epoch seconds。
     */
    @Column(name = "issued_at")
    private Long issuedAt;

    /**
     * Session expire timestamp。
     *
     * Unix epoch seconds。
     *
     * TODO:
     * 正式環境建議：
     * 1d / 7d / 30d configurable。
     */
    @Column(name = "expires_at")
    private Long expiresAt;

    /**
     * 建立時間 ISO string。
     *
     * TODO:
     * 正式環境建議改：
     * LocalDateTime / Instant。
     */
    @Column(
            name = "created_at",
            nullable = false,
            length = 64
    )
    private String createdAt;

    /**
     * confirm session 時間。
     */
    @Column(
            name = "confirmed_at",
            length = 64
    )
    private String confirmedAt;

    /**
     * revoke session 時間。
     */
    @Column(
            name = "revoked_at",
            length = 64
    )
    private String revokedAt;

    /**
     * 最後使用時間。
     *
     * TODO:
     * placeOrder 成功後更新。
     *
     * 可用於：
     * 1. inactive session cleanup
     * 2. suspicious behavior detect
     * 3. device activity tracking
     */
    @Column(
            name = "last_used_at",
            length = 64
    )
    private String lastUsedAt;

    @Column(name = "max_order_usdt", precision = 38, scale = 18)
    private BigDecimal maxOrderUsdt;

    @Column(name = "daily_limit_usdt", precision = 38, scale = 18)
    private BigDecimal dailyLimitUsdt;

    @Column(name = "daily_used_usdt", precision = 38, scale = 18)
    private BigDecimal dailyUsedUsdt;

    @Column(name = "daily_reset_date", length = 16)
    private String dailyResetDate;

    @Column(name = "revoked_reason", length = 256)
    private String revokedReason;
}
