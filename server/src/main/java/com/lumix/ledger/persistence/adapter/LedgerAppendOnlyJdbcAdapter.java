package com.lumix.ledger.persistence.adapter;

import com.lumix.ledger.persistence.LedgerAppendOnlyPersistenceMapping;
import com.lumix.ledger.persistence.LedgerEntryPersistenceMapping;
import com.lumix.ledger.persistence.LedgerJournalPersistenceMapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * 最小的 ledger append persistence adapter。
 *
 * <p>這個 adapter 只在測試或受控環境中驗證 append-only DB write gate，
 * 不代表已接到正式 posting runtime；任何正式接線都屬於 HUMAN_REVIEW_REQUIRED。</p>
 */
public final class LedgerAppendOnlyJdbcAdapter {

    private final LedgerConnectionProvider connectionProvider;

    /**
     * 建立只負責 append 的 JDBC adapter。
     *
     * <p>這裡只注入連線供應器，不注入 posting service、repository 或任何 balance mutation 路徑。</p>
     */
    public LedgerAppendOnlyJdbcAdapter(LedgerConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    /**
     * 將 mapping contract append 到 ledger_journals 與 ledger_entries。
     *
     * <p>這個方法只做最小 DB write gate，不更新 balance read model，也不自行做 business decision。
     * 呼叫端必須先完成 prerequisite gate 與 invariant check。</p>
     *
     * @return 新增的 ledger_journal_id
     */
    public long append(LedgerAppendOnlyPersistenceMapping mapping) {
        Objects.requireNonNull(mapping, "mapping must not be null");
        Objects.requireNonNull(mapping.journal(), "journal must not be null");

        List<LedgerEntryPersistenceMapping> entries = Objects.requireNonNull(mapping.entries(), "entries must not be null");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
        if (entries.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("entries must not contain null element");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionStarted = false;

            try {
                connection.setAutoCommit(false);
                transactionStarted = true;

                long ledgerJournalId = insertJournal(connection, mapping.journal());
                insertEntries(connection, ledgerJournalId, entries);

                connection.commit();
                return ledgerJournalId;
            } catch (SQLException | RuntimeException ex) {
                if (transactionStarted) {
                    rollbackQuietly(connection);
                }

                if (ex instanceof SQLException sqlException) {
                    throw new IllegalStateException("Failed to append ledger journal draft.", sqlException);
                }
                throw ex;
            } finally {
                restoreAutoCommitQuietly(connection, originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to acquire connection for ledger append gate.", ex);
        }
    }

    private long insertJournal(Connection connection, LedgerJournalPersistenceMapping journal) throws SQLException {
        // 這裡只 append journal header，避免把 adapter 誤寫成完整 posting runtime。
        final String sql = "INSERT INTO ledger_journals "
                + "(business_reference_type, business_reference_id, request_id, journal_note, posted_at) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, journal.businessReferenceType().name());
            statement.setString(2, journal.businessReferenceId());

            if (journal.requestId() == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, journal.requestId());
            }

            if (journal.journalNote() == null) {
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(4, journal.journalNote());
            }

            statement.setObject(5, OffsetDateTime.ofInstant(journal.postedAt(), ZoneOffset.UTC));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new IllegalStateException("Ledger journal id was not returned after insert.");
    }

    private void insertEntries(Connection connection, long ledgerJournalId, List<LedgerEntryPersistenceMapping> entries)
            throws SQLException {
        // 這裡只 append entry rows，避免把 adapter 誤接成任何 balance mutation 路徑。
        final String sql = "INSERT INTO ledger_entries "
                + "(ledger_journal_id, entry_sequence, account_id, asset_symbol, direction, amount) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (LedgerEntryPersistenceMapping entry : entries) {
                statement.setLong(1, ledgerJournalId);
                statement.setLong(2, entry.entrySequence());
                statement.setString(3, entry.accountId().value());
                statement.setString(4, entry.assetSymbol().value());
                statement.setString(5, entry.direction().name());
                statement.setBigDecimal(6, entry.amount());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // rollback 只是在失敗路徑維持 ledger append atomicity，不應掩蓋原始例外。
        }
    }

    private static void restoreAutoCommitQuietly(Connection connection, boolean originalAutoCommit) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException ignored) {
            // autoCommit 還原只屬於收尾保護，不能掩蓋前面的 append 結果。
        }
    }
}
