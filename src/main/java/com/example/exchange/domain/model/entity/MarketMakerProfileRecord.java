/*
 * 檔案用途：JPA entity，保存做市商 profile 主檔。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "market_maker_profiles",
        indexes = {
                @Index(name = "idx_market_maker_profiles_uid", columnList = "uid"),
                @Index(name = "idx_market_maker_profiles_enabled", columnList = "enabled")
        }
)
public class MarketMakerProfileRecord {

    @Id
    @Column(name = "market_maker_id", nullable = false, length = 128)
    private String marketMakerId;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    public static MarketMakerProfileRecord from(MarketMakerProfile profile, int schemaVersion) {
        MarketMakerProfileRecord record = new MarketMakerProfileRecord();
        record.setMarketMakerId(profile.marketMakerId());
        record.setSchemaVersion(schemaVersion);
        record.setUid(profile.uid());
        record.setEnabled(profile.enabled());
        return record;
    }
}
