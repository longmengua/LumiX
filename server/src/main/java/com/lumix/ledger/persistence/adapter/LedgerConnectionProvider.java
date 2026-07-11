package com.lumix.ledger.persistence.adapter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ledger append adapter 使用的連線供應契約。
 *
 * <p>這個介面只負責提供 JDBC Connection，不代表已接到正式 posting runtime。
 * 任何把它接到正式資金路徑的變更都屬於 HUMAN_REVIEW_REQUIRED。</p>
 */
@FunctionalInterface
public interface LedgerConnectionProvider {

    /**
     * 取得可用的 JDBC 連線。
     *
     * <p>呼叫端只應用來執行最小 append gate，不得在這裡夾帶任何 business decision。</p>
     */
    Connection getConnection() throws SQLException;
}
