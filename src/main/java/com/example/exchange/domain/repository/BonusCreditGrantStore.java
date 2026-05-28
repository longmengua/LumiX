/*
 * 檔案用途：Repository 介面，定義體驗金批次 read model 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.BonusCreditGrant;

import java.time.Instant;
import java.util.List;

public interface BonusCreditGrantStore {

    int SCHEMA_VERSION = 1;

    void save(BonusCreditGrant grant);

    List<BonusCreditGrant> findActiveByUidAndAsset(long uid, String asset);

    List<BonusCreditGrant> findActiveExpiringAtOrBefore(Instant now, int limit);

    List<BonusCreditGrant> findByUid(long uid);
}
