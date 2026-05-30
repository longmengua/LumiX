/*
 * 檔案用途：Repository 介面，保存與查詢 trial balance 每日快照。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.TrialBalanceSnapshot;

import java.time.LocalDate;
import java.util.Optional;

public interface TrialBalanceSnapshotStore {

    TrialBalanceSnapshot save(TrialBalanceSnapshot snapshot);

    Optional<TrialBalanceSnapshot> find(LocalDate reportDate, long uid, String asset);
}
