package com.lumix.infrastructure.database;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 依 Spring transaction 的 readOnly 語意選擇資料來源。
 *
 * <p>沒有交易或不是 readOnly 的操作一律走 primary，避免 command、Flyway 或未知呼叫被送到 replica。
 * repository 的查詢要使用 {@code @Transactional(readOnly = true)} 才能取得 reader routing。</p>
 */
final class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? DatabaseRole.READ : DatabaseRole.WRITE;
    }

    enum DatabaseRole {
        WRITE,
        READ
    }
}
