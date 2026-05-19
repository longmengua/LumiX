/*
 * 檔案用途：Repository 介面，定義 account risk snapshot 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.AccountRiskSnapshot;

import java.util.List;
import java.util.Optional;

public interface AccountRiskSnapshotStore {

    int SCHEMA_VERSION = 1;

    void save(AccountRiskSnapshot snapshot);

    Optional<AccountRiskSnapshot> findLatest(long uid);

    List<AccountRiskSnapshot> findByUid(long uid, int limit);
}
