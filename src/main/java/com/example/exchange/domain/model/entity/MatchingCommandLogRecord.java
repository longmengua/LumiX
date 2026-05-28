/*
 * 檔案用途：JPA entity，保存 durable matching command log。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "matching_command_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_matching_command_symbol_offset", columnNames = {"symbol_code", "offset_value"})
        },
        indexes = {
                @Index(name = "idx_matching_command_symbol_created", columnList = "symbol_code,created_at")
        }
)
public class MatchingCommandLogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "symbol_code", nullable = false, length = 32)
    private String symbolCode;

    @Column(name = "offset_value", nullable = false)
    private Long offsetValue;

    @Column(name = "command_type", nullable = false, length = 32)
    private String commandType;

    @Column(name = "order_payload", nullable = false, columnDefinition = "JSON")
    private String orderPayload;

    @Column(name = "replacement_order_payload", columnDefinition = "JSON")
    private String replacementOrderPayload;

    @Column(name = "new_price", precision = 38, scale = 18)
    private BigDecimal newPrice;

    @Column(name = "new_qty", precision = 38, scale = 18)
    private BigDecimal newQty;

    @Column(name = "owner_id", length = 128)
    private String ownerId;

    @Column(name = "owner_epoch", nullable = false)
    private Long ownerEpoch;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;
}
