package com.lumix.trading.core.projection.runtime;

import com.lumix.ledger.persistence.adapter.LedgerConnectionProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * balance projection rebuild runtime gate。
 *
 * 這個 gate 只把 ledger_entries 重建成 balance_projections read model，不代表已經有正式 balance mutation runtime。
 * 目前只 materialize SPOT account 的投影 rows，避免把 futures / margin 邊界提早混進未完成的 trading core。
 */
public final class BalanceProjectionRebuildGate {

    private final LedgerConnectionProvider connectionProvider;

    /**
     * 建立 balance projection rebuild gate。
     *
     * <p>這裡只注入最小連線供應器，不接 repository、transaction annotation 或任何正式 money movement runtime。</p>
     */
    public BalanceProjectionRebuildGate(LedgerConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    /**
     * 以 ledger_entries 重建 balance_projections read model。
     *
     * <p>這個方法只做保守的 SPOT-only rebuild gate：
     * 先讀 ledger_entries，再把 SPOT account 的 account_id + asset_symbol 投影重建到 balance_projections。
     * CREDIT 會增加 total_amount，DEBIT 會減少 total_amount；available_amount 暫時等於 total_amount，locked_amount 暫時固定為 0。</p>
     *
     * @param projectedAt 本次重建批次的投影時間點，用來同步 projected_at / reconciled_at。
     * @param projectionVersion 本次重建批次版本，用來寫入 projection_version。
     * @return 本次重建結果，只表示 read model 批次完成。
     */
    public BalanceProjectionRebuildResult rebuild(Instant projectedAt, long projectionVersion) {
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
        if (projectionVersion < 0L) {
            throw new IllegalArgumentException("projectionVersion must not be negative");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            boolean transactionStarted = false;

            try {
                connection.setAutoCommit(false);
                transactionStarted = true;

                List<ProjectionRow> projectedRows = loadProjectedRows(connection);
                validateProjectedRows(projectedRows);

                deleteSpotBalanceProjections(connection);
                int insertedRowCount = insertBalanceProjections(connection, projectedRows, projectedAt, projectionVersion);

                connection.commit();
                return new BalanceProjectionRebuildResult(insertedRowCount, projectionVersion, projectedAt);
            } catch (SQLException | RuntimeException ex) {
                if (transactionStarted) {
                    rollbackQuietly(connection);
                }

                if (ex instanceof SQLException sqlException) {
                    throw new IllegalStateException("Failed to rebuild balance projection read model.", sqlException);
                }
                throw ex;
            } finally {
                restoreAutoCommitQuietly(connection, originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to acquire connection for balance projection rebuild gate.", ex);
        }
    }

    /**
     * 讀取需要 materialize 到 read model 的 SPOT projection rows。
     *
     * <p>這裡直接從 ledger_entries 彙總 account_id + asset_symbol，並用 account_assets / accounts boundary
     * 只保留目前可投影的 SPOT 帳戶。</p>
     */
    private static List<ProjectionRow> loadProjectedRows(Connection connection) throws SQLException {
        final String sql = """
                SELECT e.account_id,
                       e.asset_symbol,
                       SUM(CASE
                               WHEN e.direction = 'CREDIT' THEN e.amount
                               ELSE -e.amount
                           END) AS total_amount
                FROM ledger_entries e
                JOIN account_assets aa
                  ON aa.account_id = e.account_id
                 AND aa.asset_symbol = e.asset_symbol
                JOIN accounts a
                  ON a.account_id = e.account_id
                 AND a.account_type = 'SPOT'
                GROUP BY e.account_id, e.asset_symbol
                ORDER BY e.account_id, e.asset_symbol
                """;

        List<ProjectionRow> projectedRows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                projectedRows.add(new ProjectionRow(
                        resultSet.getString("account_id"),
                        resultSet.getString("asset_symbol"),
                        resultSet.getBigDecimal("total_amount")
                ));
            }
        }

        return projectedRows;
    }

    /**
     * 檢查 projected total 是否符合目前 read model schema。
     *
     * <p>如果彙總結果出現負值，代表 ledger data 與目前的 balance projection contract 不一致，
     * 這次 rebuild 必須失敗，避免把不完整的 read model 寫回去。</p>
     */
    private static void validateProjectedRows(List<ProjectionRow> projectedRows) {
        for (ProjectionRow row : projectedRows) {
            if (row.totalAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Projected total amount must not be negative for account "
                                + row.accountId()
                                + " and asset "
                                + row.assetSymbol()
                );
            }
        }
    }

    /**
     * 在重建前先刪除既有 SPOT projection rows。
     *
     * <p>這是保守的 SPOT-only rebuild gate，所以只清掉 SPOT rows，不動 MARGIN / FUTURES / 其他 account type 的 projection rows。</p>
     */
    private static void deleteSpotBalanceProjections(Connection connection) throws SQLException {
        final String sql = "DELETE FROM " + "balance_projections bp "
                + "WHERE EXISTS ("
                + "SELECT 1 "
                + "FROM accounts a "
                + "WHERE a.account_id = bp.account_id "
                + "AND a.account_type = 'SPOT')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    /**
     * 將 projection rows 寫回 balance_projections。
     *
     * <p>這裡只寫 query-side read model，不觸碰 ledger tables，也不更新任何 source of truth。</p>
     */
    private static int insertBalanceProjections(
            Connection connection,
            List<ProjectionRow> projectedRows,
            Instant projectedAt,
            long projectionVersion
    ) throws SQLException {
        final String sql = "INSERT INTO " + "balance_projections "
                + "(account_id, asset_symbol, total_amount, available_amount, locked_amount, projection_version, projected_at, reconciled_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int insertedRowCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ProjectionRow row : projectedRows) {
                statement.setString(1, row.accountId());
                statement.setString(2, row.assetSymbol());
                statement.setBigDecimal(3, row.totalAmount());
                statement.setBigDecimal(4, row.totalAmount());
                statement.setBigDecimal(5, BigDecimal.ZERO);
                statement.setLong(6, projectionVersion);
                statement.setObject(7, OffsetDateTime.ofInstant(projectedAt, ZoneOffset.UTC));
                statement.setObject(8, OffsetDateTime.ofInstant(projectedAt, ZoneOffset.UTC));
                statement.addBatch();
                insertedRowCount++;
            }
            statement.executeBatch();
        }

        return insertedRowCount;
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // rollback 只是在失敗路徑維持 projection rebuild atomicity，不應掩蓋原始例外。
        }
    }

    private static void restoreAutoCommitQuietly(Connection connection, boolean originalAutoCommit) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException ignored) {
            // autoCommit 還原只屬於收尾保護，不能掩蓋前面的 rebuild 結果。
        }
    }

    private record ProjectionRow(String accountId, String assetSymbol, BigDecimal totalAmount) {
    }
}
