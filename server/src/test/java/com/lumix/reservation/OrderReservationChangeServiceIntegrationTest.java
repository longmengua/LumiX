package com.lumix.reservation;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.*;
import com.lumix.ledger.runtime.*;
import com.lumix.testing.JdbcDataSource;
import com.lumix.trading.runtime.TradingOrderQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 真實 PostgreSQL 驗證共用交易資金前置，不以 mocked SQL 或 sandbox 代替並行與回滾。
 * fixture 僅存在測試 source set；所有資料都寫入專用 lumix_test schema。
 */
class OrderReservationChangeServiceIntegrationTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private OrderReservationChangeService service;
    private TransactionalLedgerPostingService ledger;

    @BeforeEach
    void initializeIsolatedOrderFunds() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:postgresql:test:order_reservation_change");
        Flyway migration = Flyway.configure().dataSource(source).locations("classpath:db/migration").cleanDisabled(false).load();
        migration.clean();
        migration.migrate();
        jdbc = new JdbcTemplate(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        service = new OrderReservationChangeService(jdbc, new ObjectMapper());
        ledger = new TransactionalLedgerPostingService(jdbc, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbc));
        jdbc.update("INSERT INTO users (user_id,email,display_name,status) VALUES "
                + "('order-owner','order-owner@example.test','Order Owner','ACTIVE'),"
                + "('other-owner','other-owner@example.test','Other Owner','ACTIVE'),"
                + "('system:order-test','order-system@example.test','System','SUSPENDED')");
        jdbc.update("INSERT INTO accounts (account_id,user_id,account_type,status,account_category) VALUES "
                + "('owner:spot','order-owner','SPOT','ACTIVE','USER'),"
                + "('owner:futures','order-owner','FUTURES','ACTIVE','USER'),"
                + "('other:spot','other-owner','SPOT','ACTIVE','USER'),"
                + "('order:exchange','system:order-test','SPOT','ACTIVE','EXCHANGE')");
        jdbc.update("INSERT INTO assets (asset_symbol,display_name,precision_scale,status) VALUES "
                + "('BTC','Bitcoin',8,'ACTIVE'),('USDT','Tether',6,'ACTIVE')");
        jdbc.update("INSERT INTO markets (market_symbol,base_asset_symbol,quote_asset_symbol,price_scale,quantity_scale,status) "
                + "VALUES ('BTC-USDT','BTC','USDT',2,6,'ACTIVE')");
        jdbc.update("INSERT INTO account_assets (account_id,asset_symbol,status) VALUES "
                + "('owner:spot','USDT','ACTIVE'),('owner:futures','USDT','ACTIVE'),('order:exchange','USDT','ACTIVE')");
        transaction.executeWithoutResult(status -> {
            post("seed:spot", "order:exchange", "owner:spot", "100");
            post("seed:futures", "order:exchange", "owner:futures", "100");
        });
        createOrderAndHold("spot-order", "owner:spot", "spot-hold");
        createOrderAndHold("futures-order", "owner:futures", "futures-hold");
    }

    /** 部分成交只扣本次金額，價格改善只釋放差額，最後撤單不可把已成交金額再次退款。 */
    @Test
    void partialCapturePriceImprovementAndCancellationKeepMoneyConserved() {
        transaction.executeWithoutResult(status -> {
            OrderReservationChangeResult first = service.change(command(OrderReservationChange.Action.CAPTURE, "30", "fill-1"));
            assertResult(first, "50", "30", "0", "PARTIALLY_CONSUMED", false);
            post("fill-1", "owner:spot", "order:exchange", "30");
            service.change(command(OrderReservationChange.Action.RELEASE, "5", "price-improvement-1"));
        });
        assertBalance("owner:spot", "70", "25", "45");
        OrderReservationChangeResult cancel = transaction.execute(status ->
                service.change(command(OrderReservationChange.Action.RELEASE, "45", "cancel-1")));
        assertResult(cancel, "0", "30", "50", "RELEASED", false);
        assertBalance("owner:spot", "70", "70", "0");
        assertEquals(3L, count("audit_logs", "action_type LIKE 'ORDER_RESERVATION_%'"));
        assertEquals(3L, count("outbox_events", "event_type LIKE 'ORDER_RESERVATION_%'"));
        assertEquals(0, new BigDecimal("100").compareTo(jdbc.queryForObject(
                "SELECT total_amount + (SELECT COALESCE(SUM(amount),0) FROM ledger_entries WHERE account_id='owner:spot' AND direction='DEBIT') "
                        + "FROM balance_projections WHERE account_id='owner:spot'", BigDecimal.class)));
    }

    /** 同鍵等值金額換追蹤 ID仍回放原結果；後續狀態改變不能使舊重送再次扣款。 */
    @Test
    void replayUsesOriginalSnapshotAndRejectsPayloadChanges() {
        OrderReservationChange original = command(OrderReservationChange.Action.CAPTURE, "30.00", "stable-key");
        OrderReservationChangeResult first = transaction.execute(status -> {
            var result = service.change(original);
            post("stable-fill", "owner:spot", "order:exchange", "30");
            return result;
        });
        transaction.executeWithoutResult(status -> service.change(command(OrderReservationChange.Action.RELEASE, "50", "cancel-rest")));
        jdbc.update("UPDATE orders SET status='CANCELED' WHERE order_id='spot-order'");
        OrderReservationChange retry = new OrderReservationChange(original.action(), original.ownerUserId(), original.accountId(),
                original.orderId(), original.reservationId(), original.assetSymbol(), new BigDecimal("30.0"), "new-request", original.idempotencyKey());
        OrderReservationChangeResult replay = transaction.execute(status -> service.change(retry));
        assertResult(replay, "50", "30", "0", "PARTIALLY_CONSUMED", true);
        assertEquals(first.remainingAmount(), replay.remainingAmount());
        assertThrows(IllegalArgumentException.class, () -> transaction.execute(status ->
                service.change(command(OrderReservationChange.Action.CAPTURE, "31", "stable-key"))));
        assertBalance("owner:spot", "70", "70", "0");
        assertEquals(2L, count("audit_logs", "action_type LIKE 'ORDER_RESERVATION_%'"));
        // 持久化重播證據損毀時不可猜測成功或重新執行；只在隔離 fixture 模擬損毀。
        jdbc.update("UPDATE idempotency_keys SET response_summary='{}' WHERE scope='RESERVATION_CAPTURE' AND idempotency_key LIKE 'order:%'");
        assertThrows(RuntimeException.class, () -> transaction.execute(status -> service.change(retry)));
        assertBalance("owner:spot", "70", "70", "0");
    }

    /** 同一實作可處理合約帳戶的訂單預留，但不代表持倉／保證金風險 runtime 已完成。 */
    @Test
    void futuresOrderUsesItsOwnAccountAndRetainsUnfilledHold() {
        transaction.executeWithoutResult(status -> {
            OrderReservationChange command = new OrderReservationChange(OrderReservationChange.Action.CAPTURE,
                    "order-owner", "owner:futures", "futures-order", "futures-hold", "USDT", new BigDecimal("20"), "futures-fill", "futures-fill");
            assertResult(service.change(command), "60", "20", "0", "PARTIALLY_CONSUMED", false);
            post("futures-fill", "owner:futures", "order:exchange", "20");
        });
        assertBalance("owner:futures", "80", "20", "60");
        assertBalance("owner:spot", "100", "20", "80");
    }

    /** Owner、訂單、資產或用途不匹配皆拒絕，不能以合法 reservation ID挪用另一條資金路徑。 */
    @Test
    void rejectsOwnershipPurposeAssetAndAmountViolationsWithoutClaimResidue() {
        List<OrderReservationChange> invalid = List.of(
                new OrderReservationChange(OrderReservationChange.Action.CAPTURE, "other-owner", "owner:spot", "spot-order", "spot-hold", "USDT", BigDecimal.ONE, "bad-owner", "bad-owner"),
                new OrderReservationChange(OrderReservationChange.Action.CAPTURE, "order-owner", "owner:spot", "spot-order", "futures-hold", "USDT", BigDecimal.ONE, "bad-order", "bad-order"),
                new OrderReservationChange(OrderReservationChange.Action.CAPTURE, "order-owner", "owner:spot", "spot-order", "spot-hold", "BTC", BigDecimal.ONE, "bad-asset", "bad-asset"),
                command(OrderReservationChange.Action.CAPTURE, "0.0000001", "bad-precision"),
                command(OrderReservationChange.Action.CAPTURE, "81", "too-much"));
        for (OrderReservationChange command : invalid) {
            assertThrows(RuntimeException.class, () -> transaction.execute(status -> service.change(command)));
        }
        jdbc.update("UPDATE reservations SET business_reference_type='WITHDRAWAL', reservation_type='WITHDRAWAL_HOLD' WHERE reservation_id='spot-hold'");
        assertThrows(IllegalArgumentException.class, () -> transaction.execute(status -> service.change(command(OrderReservationChange.Action.CAPTURE, "1", "bad-purpose"))));
        assertBalance("owner:spot", "100", "20", "80");
        assertEquals(0L, count("idempotency_keys", "idempotency_key LIKE 'order:%'"));
    }

    /** 後續結算失敗時，partial mutation、投影、冪等與證據必須一起回滾，之後相同鍵仍可重試。 */
    @Test
    void settlementFailureRollsBackAllEffectsAndAllowsRetry() {
        long journals = count("ledger_journals", "TRUE");
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.change(command(OrderReservationChange.Action.CAPTURE, "30", "rollback-key"));
            post("rollback-fill", "owner:spot", "order:exchange", "30");
            throw new IllegalStateException("downstream trade write failed");
        }));
        assertBalance("owner:spot", "100", "20", "80");
        assertEquals(journals, count("ledger_journals", "TRUE"));
        assertEquals(0L, count("idempotency_keys", "idempotency_key LIKE 'order:%'"));
        assertEquals(0L, count("outbox_events", "event_type LIKE 'ORDER_RESERVATION_%'"));
        transaction.executeWithoutResult(status -> {
            assertFalse(service.change(command(OrderReservationChange.Action.CAPTURE, "30", "rollback-key")).replayed());
            post("rollback-fill", "owner:spot", "order:exchange", "30");
        });
        assertBalance("owner:spot", "70", "20", "50");
    }

    /** 同時扣用同一筆資金，僅一個超額命令可成功；不同命令不能各自讀到相同 remaining。 */
    @Test
    void concurrentCaptureAndReleaseCannotOverdrawRemaining() throws Exception {
        int captured;
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var capture = executor.submit(() -> race(start, OrderReservationChange.Action.CAPTURE, "race-capture"));
            var release = executor.submit(() -> race(start, OrderReservationChange.Action.RELEASE, "race-release"));
            start.countDown();
            captured = capture.get(10, TimeUnit.SECONDS);
            assertEquals(1, captured + release.get(10, TimeUnit.SECONDS));
        }
        assertBalance("owner:spot", captured == 1 ? "40" : "100", captured == 1 ? "20" : "80", "20");
        assertEquals(0, new BigDecimal("20").compareTo(jdbc.queryForObject("SELECT remaining_amount FROM reservations WHERE reservation_id='spot-hold'", BigDecimal.class)));
        assertEquals(1L, count("audit_logs", "action_type LIKE 'ORDER_RESERVATION_%'"));
    }

    /** 相同命令並行重試只執行一次；重播不新增 outbox 或重複解除 lock。 */
    @Test
    void concurrentDuplicateCommandIsAppliedOnlyOnce() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            java.util.concurrent.Callable<OrderReservationChangeResult> task = () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return transaction.execute(status -> service.change(command(OrderReservationChange.Action.RELEASE, "30", "duplicate")));
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS).replayed(), second.get(10, TimeUnit.SECONDS).replayed());
        }
        assertBalance("owner:spot", "100", "50", "50");
        assertEquals(1L, count("outbox_events", "event_type LIKE 'ORDER_RESERVATION_%'"));
    }

    /** 內部步驟不可自行開交易提交，已終止訂單也不能用新鍵再次執行扣用。 */
    @Test
    void requiresOuterWriteTransactionAndExecutableOrder() {
        assertThrows(IllegalStateException.class, () -> service.change(command(OrderReservationChange.Action.CAPTURE, "1", "no-transaction")));
        TransactionTemplate readOnly = new TransactionTemplate(transaction.getTransactionManager());
        readOnly.setReadOnly(true);
        assertThrows(IllegalStateException.class, () -> readOnly.execute(status -> service.change(command(OrderReservationChange.Action.CAPTURE, "1", "read-only"))));
        jdbc.update("UPDATE orders SET status='CANCELED' WHERE order_id='spot-order'");
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> service.change(command(OrderReservationChange.Action.CAPTURE, "1", "terminal"))));
        assertBalance("owner:spot", "100", "20", "80");
    }

    /** 查詢不得跨 owner 或現貨／合約帳戶；以真實訂單讀取，不使用 repository mock。 */
    @Test
    void orderQueriesKeepOwnerAndAccountKindIsolated() {
        TradingOrderQueryService queries = new TradingOrderQueryService(jdbc);
        assertEquals("spot-order", queries.list("order-owner", "spot", 20, null).items().getFirst().orderId());
        assertEquals("futures-order", queries.list("order-owner", "futures", 20, null).items().getFirst().orderId());
        assertTrue(queries.list("other-owner", "spot", 20, null).items().isEmpty());
        assertThrows(java.util.NoSuchElementException.class, () -> queries.detail("other-owner", "spot", "spot-order"));
        assertThrows(java.util.NoSuchElementException.class, () -> queries.detail("order-owner", "futures", "spot-order"));
        assertEquals("10", queries.detail("order-owner", "spot", "spot-order").price());
    }

    /** 同時間戳委託仍以 ID 穩定分頁；游標不能擴大查詢 owner，非法頁大小與游標必須拒絕。 */
    @Test
    void orderPaginationIsBoundedAndHasNoDuplicateAtTimestampTies() {
        TradingOrderQueryService queries = new TradingOrderQueryService(jdbc);
        jdbc.update("INSERT INTO orders (order_id,user_id,account_id,market_symbol,side,order_type,status,price,quantity,remaining_quantity,request_id) "
                + "VALUES ('spot-order-2','order-owner','owner:spot','BTC-USDT','BUY','LIMIT','OPEN',10,1,1,'spot-order-2')");
        jdbc.update("UPDATE orders SET created_at='2026-11-21T00:00:00Z' WHERE account_id='owner:spot'");
        var first = queries.list("order-owner", "spot", 1, null);
        var next = queries.list("order-owner", "spot", 1, first.nextCursor());
        assertEquals("spot-order-2", first.items().getFirst().orderId());
        assertEquals("spot-order", next.items().getFirst().orderId());
        assertNull(next.nextCursor());
        assertTrue(queries.list("other-owner", "spot", 1, first.nextCursor()).items().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> queries.list("order-owner", "spot", 101, null));
        assertThrows(IllegalArgumentException.class, () -> queries.list("order-owner", "spot", 0, null));
        assertThrows(IllegalArgumentException.class, () -> queries.list("order-owner", "spot", 20, "invalid"));
        assertThrows(IllegalArgumentException.class, () -> queries.list("order-owner", "margin", 20, null));
    }

    private int race(CountDownLatch start, OrderReservationChange.Action action, String key) throws Exception {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            transaction.executeWithoutResult(status -> {
                service.change(command(action, "60", key));
                if (action == OrderReservationChange.Action.CAPTURE) {
                    post("race-fill", "owner:spot", "order:exchange", "60");
                }
            });
            return 1;
        } catch (IllegalStateException rejected) {
            return 0;
        }
    }

    private void createOrderAndHold(String order, String account, String reservation) {
        jdbc.update("INSERT INTO orders (order_id,user_id,account_id,market_symbol,side,order_type,time_in_force,status,price,quantity,remaining_quantity,request_id) "
                        + "VALUES (?,'order-owner',?,'BTC-USDT','BUY','LIMIT','GTC','OPEN',10,8,8,?)", order, account, order);
        transaction.executeWithoutResult(status -> new ReservationHoldService(jdbc).hold(new ReservationHoldCommand(
                reservation, account, "USDT", order, new BigDecimal("80"), "hold:" + order, "hold:" + order, "order-owner", "ORDER", "ORDER_HOLD")));
    }

    private OrderReservationChange command(OrderReservationChange.Action action, String amount, String key) {
        return new OrderReservationChange(action, "order-owner", "owner:spot", "spot-order", "spot-hold", "USDT", new BigDecimal(amount), key, key);
    }

    private void post(String reference, String debit, String credit, String amount) {
        ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId(reference),
                new LedgerJournalDraft(LedgerBusinessReferenceType.SETTLEMENT, reference, List.of(
                        new LedgerEntryDraft(new AccountId(debit), new AssetSymbol("USDT"), LedgerDirection.DEBIT, new BigDecimal(amount), 1),
                        new LedgerEntryDraft(new AccountId(credit), new AssetSymbol("USDT"), LedgerDirection.CREDIT, new BigDecimal(amount), 2))), Instant.now()),
                reference, new LedgerPostingActor("SYSTEM", "test")));
    }

    private void assertBalance(String account, String total, String available, String locked) {
        var row = jdbc.queryForMap("SELECT total_amount,available_amount,locked_amount FROM balance_projections WHERE account_id=? AND asset_symbol='USDT'", account);
        assertEquals(0, new BigDecimal(total).compareTo((BigDecimal) row.get("total_amount")));
        assertEquals(0, new BigDecimal(available).compareTo((BigDecimal) row.get("available_amount")));
        assertEquals(0, new BigDecimal(locked).compareTo((BigDecimal) row.get("locked_amount")));
    }

    private void assertResult(OrderReservationChangeResult result, String remaining, String consumed, String released, String status, boolean replay) {
        assertNotNull(result);
        assertEquals(0, new BigDecimal(remaining).compareTo(result.remainingAmount()));
        assertEquals(0, new BigDecimal(consumed).compareTo(result.consumedAmount()));
        assertEquals(0, new BigDecimal(released).compareTo(result.releasedAmount()));
        assertEquals(status, result.status());
        assertEquals(replay, result.replayed());
    }

    private long count(String table, String condition) {
        // table／condition 只有本檔固定常數，不接受 application 或外部輸入。
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + condition, Long.class);
    }
}
