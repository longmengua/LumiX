/*
 * 檔案用途：Repository 介面，定義 turnover read model 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.TurnoverRecord;

import java.util.List;

public interface TurnoverStore {

    int SCHEMA_VERSION = 1;

    void append(TurnoverRecord record);

    List<TurnoverRecord> findByUid(long uid);

    List<TurnoverRecord> findByMatchId(String matchId);
}
