/*
 * 檔案用途：Repository 介面，定義做市商 profile 與 risk limit 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MarketMakerProfile;

import java.util.List;
import java.util.Optional;

public interface MarketMakerProfileStore {

    int SCHEMA_VERSION = 1;

    void save(MarketMakerProfile profile);

    Optional<MarketMakerProfile> findByMarketMakerId(String marketMakerId);

    Optional<MarketMakerProfile> findByUid(long uid);

    /** Lists all profiles so disabled market makers remain visible and can be re-enabled by operators. */
    List<MarketMakerProfile> findAll();

    List<MarketMakerProfile> findEnabled();
}
