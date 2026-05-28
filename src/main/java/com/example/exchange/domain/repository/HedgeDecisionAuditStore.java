/*
 * 檔案用途：Repository 介面，定義 hedge decision audit trail 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;

import java.util.List;

public interface HedgeDecisionAuditStore {

    int SCHEMA_VERSION = 1;

    void append(HedgeDecisionAuditRecord record);

    List<HedgeDecisionAuditRecord> findByMarketMakerId(String marketMakerId, int limit);

    List<HedgeDecisionAuditRecord> findByRefId(String refId);
}
