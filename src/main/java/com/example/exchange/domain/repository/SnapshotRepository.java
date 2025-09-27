package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.Snapshot;

import java.util.Optional;

/**
 * 快照儲存抽象
 *
 * - 保存聚合後的帳戶/倉位/訂單等狀態
 * - 恢復時可快速載入，再接續回放增量事件
 */
public interface SnapshotRepository {

    /** 儲存最新快照（通常以 uid 為 key 覆蓋） */
    void save(Snapshot snapshot);

    /** 取得某使用者的最新快照 */
    Optional<Snapshot> latest(long uid);
}
