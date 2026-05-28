/*
 * 檔案用途：JPA entity，保存體驗金 grant 批次與 remaining 狀態。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.BonusCreditGrant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "bonus_credit_grants",
        indexes = {
                @Index(name = "idx_bonus_credit_uid_asset_status", columnList = "uid,asset,status,expires_at"),
                @Index(name = "idx_bonus_credit_expiry", columnList = "status,expires_at"),
                @Index(name = "idx_bonus_credit_campaign", columnList = "campaign_id")
        }
)
public class BonusCreditGrantRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "asset", nullable = false, length = 32)
    private String asset;

    @Column(name = "original_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal originalAmount;

    @Column(name = "remaining_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal remainingAmount;

    @Column(name = "campaign_id", length = 128)
    private String campaignId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "granted_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant grantedAt;

    @Column(name = "expires_at", columnDefinition = "DATETIME(6)")
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static BonusCreditGrantRecord from(BonusCreditGrant grant, int schemaVersion) {
        BonusCreditGrantRecord record = new BonusCreditGrantRecord();
        record.setId(grant.id().toString());
        record.setSchemaVersion(schemaVersion);
        record.setUid(grant.uid());
        record.setAsset(grant.asset());
        record.setOriginalAmount(grant.originalAmount());
        record.setRemainingAmount(grant.remainingAmount());
        record.setCampaignId(grant.campaignId());
        record.setStatus(grant.status());
        record.setGrantedAt(grant.grantedAt());
        record.setExpiresAt(grant.expiresAt());
        record.setUpdatedAt(grant.updatedAt());
        return record;
    }

    public BonusCreditGrant toGrant() {
        return new BonusCreditGrant(
                UUID.fromString(id),
                uid,
                asset,
                originalAmount,
                remainingAmount,
                campaignId,
                status,
                grantedAt,
                expiresAt,
                updatedAt
        );
    }
}
