/*
 * 檔案用途：Repository 介面，定義 hedge fill audit trail 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.HedgeFillRecord;

import java.util.List;

public interface HedgeFillStore {

    int SCHEMA_VERSION = 1;

    void append(HedgeFillRecord record);

    List<HedgeFillRecord> findByMarketMakerId(String marketMakerId, int limit);

    List<HedgeFillRecord> findByVenueOrderId(String venueOrderId);

    List<HedgeFillRecord> findByRefId(String refId);
}
