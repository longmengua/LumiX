/*
 * 檔案用途：JPA entity，保存 insurance fund capital movement records。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.InsuranceFundMovement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "insurance_fund_movements",
        indexes = {
                @Index(name = "idx_insurance_fund_asset_time", columnList = "asset,created_at"),
                @Index(name = "idx_insurance_fund_reason_time", columnList = "reason,created_at"),
                @Index(name = "idx_insurance_fund_ref", columnList = "ref_id")
        }
)
public class InsuranceFundMovementEntity {

    @Id
    @Column(name = "movement_id", nullable = false, length = 128)
    private String movementId;

    @Column(name = "asset", nullable = false, length = 32)
    private String asset;

    @Column(name = "reason", nullable = false, length = 128)
    private String reason;

    @Column(name = "ref_id", nullable = false, length = 128)
    private String refId;

    @Column(name = "amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 38, scale = 18)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static InsuranceFundMovementEntity from(InsuranceFundMovement movement) {
        InsuranceFundMovementEntity entity = new InsuranceFundMovementEntity();
        entity.setMovementId(movement.movementId());
        entity.setAsset(movement.asset());
        entity.setReason(movement.reason());
        entity.setRefId(movement.refId());
        entity.setAmount(movement.amount());
        entity.setBalanceAfter(movement.balanceAfter());
        entity.setCreatedAt(movement.createdAt());
        return entity;
    }

    public InsuranceFundMovement toMovement() {
        return new InsuranceFundMovement(
                movementId,
                asset,
                reason,
                refId,
                amount,
                balanceAfter,
                createdAt
        );
    }
}
