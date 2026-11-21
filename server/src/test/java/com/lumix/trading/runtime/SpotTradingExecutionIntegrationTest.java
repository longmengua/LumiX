package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.*;
import com.lumix.ledger.runtime.*;
import com.lumix.reservation.*;
import com.lumix.testing.JdbcDataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 僅在專用 PostgreSQL 測試 schema 建立雙邊資金；驗證真實 hold／trade／ledger 原子閉環。 */
class SpotTradingExecutionIntegrationTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private TransactionalLedgerPostingService ledger;
    private SpotTradingExecutionService engine;
    private final ObjectMapper json=new ObjectMapper();

    @BeforeEach void initializeIsolatedMarket() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:postgresql:test:spot_execution");
        Flyway migrations=Flyway.configure().dataSource(source).locations("classpath:db/migration").cleanDisabled(false).load();
        migrations.clean();migrations.migrate();
        jdbc=new JdbcTemplate(source);transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        ledger=new TransactionalLedgerPostingService(jdbc,new LedgerInvariantPolicy(),new LedgerBalanceProjectionUpdater(jdbc));
        for(String owner:List.of("alice","bob","carol","treasury","fees")) {
            boolean system=List.of("treasury","fees").contains(owner);
            jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES (?,?,?,?)",owner,owner+"@example.test",owner,system?"SUSPENDED":"ACTIVE");
            jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) VALUES (?,?,'SPOT','ACTIVE',?,?)",owner,owner,system?"EXCHANGE":"USER","fees".equals(owner)?"TRADING_FEE_REVENUE":null);
        }
        jdbc.update("INSERT INTO assets(asset_symbol,display_name,precision_scale,status) VALUES ('BTC','Bitcoin',8,'ACTIVE'),('USDT','Tether',6,'ACTIVE')");
        jdbc.update("INSERT INTO markets(market_symbol,base_asset_symbol,quote_asset_symbol,price_scale,quantity_scale,status) VALUES ('BTC-USDT','BTC','USDT',2,4,'ACTIVE')");
        for(String account:List.of("alice","bob","carol","treasury","fees"))for(String asset:List.of("BTC","USDT"))jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,?,'ACTIVE')",account,asset);
        transaction.executeWithoutResult(status->{for(String owner:List.of("alice","bob","carol")){seed(owner,"USDT","10000");seed(owner,"BTC","1");}});
        var rules=new TradingInstrumentRules("SPOT:BTCUSDT",TradingInstrumentRules.Kind.SPOT,"BTC-USDT","BTC","USDT",1,2,4,6,
                new BigDecimal("0.01"),new BigDecimal("0.0001"),new BigDecimal("0.0001"),new BigDecimal("100"),BigDecimal.ONE,new BigDecimal("10000000"),new BigDecimal("0.001"),new BigDecimal("0.002"),RoundingMode.DOWN);
        // 只有測試 source set 的具名政策；application 不會載入此零依賴 fixture。
        var policy=new TradingExecutionPolicy(rules,"fees",100,new BigDecimal("0.05"),TradingExecutionPolicy.FeeAccrual.CUMULATIVE_QUOTE,TradingExecutionPolicy.CounterpartyModel.CLIENT_TO_CLIENT);
        transaction.executeWithoutResult(status->{
            jdbc.update("INSERT INTO trading_instruments(instrument_id,instrument_kind,market_symbol,status) VALUES ('SPOT:BTCUSDT','SPOT','BTC-USDT','HALTED')");
            try {jdbc.update("INSERT INTO trading_execution_policies(instrument_id,policy_version,policy,created_by) VALUES ('SPOT:BTCUSDT',1,CAST(? AS jsonb),'test')",json.writeValueAsString(policy));}
            catch(Exception e){throw new IllegalStateException(e);}
            jdbc.update("UPDATE trading_instruments SET policy_version=1,status='ACTIVE' WHERE instrument_id='SPOT:BTCUSDT'");
        });
        engine=engine();
    }

    /** Maker 價成交、雙邊費率、部分成交及剩餘資產保留同時對帳。 */
    @Test void partialFillSettlesRealFundsAtMakerPrice() {
        var maker=place("bob","SELL","100","0.5","maker");
        var buy=place("alice","BUY","110","0.2","buy");
        assertEquals("FILLED",buy.status());assertEquals("PARTIALLY_FILLED",new TradingOrderQueryService(jdbc).detail("bob","spot",maker.orderId()).status());
        var workspace=transaction.execute(status->new TradingWorkspaceQueryService(jdbc).snapshot("alice","spot","BTC-USDT"));
        assertEquals(1,workspace.fills().size());assertEquals("BUY",workspace.fills().getFirst().side());
        assertEquals("0.04",workspace.fills().getFirst().fee());assertEquals("0.3",workspace.asks().getFirst().quantity());
        assertEquals("9979.96",workspace.balances().stream().filter(b->b.asset().equals("USDT")).findFirst().orElseThrow().total());
        balance("alice","USDT","9979.96","0");balance("alice","BTC","1.2","0");
        balance("bob","USDT","10019.98","0");balance("bob","BTC","0.8","0.3");
        assertMoney("0.06",jdbc.queryForObject("SELECT SUM(amount) FROM ledger_entries WHERE account_id='fees' AND asset_symbol='USDT'",BigDecimal.class));
        assertEquals(1,count("trades"));assertEquals(1,count("trading_trade_executions"));
        conserved("BTC");conserved("USDT");
    }

    /** 價格改善與累計費用差額只能釋放一次，撤單不退回已成交的本金。 */
    @Test void priceImprovementCancellationReplayAndRestartAreDurable() {
        var buy=place("alice","BUY","100","0.3","stable");
        place("bob","SELL","90","0.1","sell");
        balance("alice","USDT","9989.99","20.04");
        engine=engine();
        var replay=place("alice","BUY","100.00","0.3000","stable");assertEquals(buy.orderId(),replay.orderId());
        var canceled=transaction.execute(status->engine.cancel("alice",buy.orderId(),"cancel"));
        assertEquals("CANCELED",Objects.requireNonNull(canceled).status());balance("alice","USDT","9989.99","0");
        transaction.execute(status->engine.cancel("alice",buy.orderId(),"cancel"));
        assertEquals(2,count("orders"));assertEquals(1,count("trades"));conserved("USDT");
    }

    /** FOK 流動性不足時連同已嘗試的成交全部回滾，不能留下對手方半筆成交。 */
    @Test void fillOrKillAndSelfTradeFailuresLeaveNoPartialFinancialEffects() {
        var maker=place("bob","SELL","100","0.1","maker");
        assertThrows(TradingOrderRejectedException.class,()->transaction.execute(status->engine.place("alice",new SpotTradingExecutionService.Command("BTC-USDT","BUY","LIMIT","FOK","100","0.5"),"fok")));
        assertEquals(1,count("orders"));assertEquals(0,count("trades"));
        assertEquals("OPEN",new TradingOrderQueryService(jdbc).detail("bob","spot",maker.orderId()).status());
        assertThrows(TradingOrderRejectedException.class,()->place("bob","BUY","100","0.1","self"));
        assertEquals(1,count("orders"));balance("bob","BTC","1","0.1");balance("alice","USDT","10000","0");
    }

    /** 市價只吃 LumiX 真實委託；不足的 IOC 餘量釋放，沒有簿時不以外部價格補成交。 */
    @Test void marketIocUsesOnlyActualLiquidityAndReleasesRemainder() {
        place("bob","SELL","100","0.1","maker");
        var result=transaction.execute(status->engine.place("alice",new SpotTradingExecutionService.Command("BTC-USDT","BUY","MARKET","IOC",null,"0.5"),"market"));
        assertEquals("CANCELED",Objects.requireNonNull(result).status());assertMoney("0.1",new BigDecimal(result.filledQuantity()));
        balance("alice","USDT","9989.98","0");assertEquals(1,count("trades"));
        assertThrows(TradingOrderRejectedException.class,()->transaction.execute(status->engine.place("carol",new SpotTradingExecutionService.Command("BTC-USDT","BUY","MARKET","IOC",null,"0.1"),"empty")));
        assertEquals(2,count("orders"));
    }

    /** 相同餘額的並行新單只准一筆取得足額保留，失敗命令不留下孤兒訂單或 reservation。 */
    @Test void concurrentOrdersCannotOverspendAndOwnerCannotCancelOthers() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(int i=0;i<2;i++){String key="concurrent-"+i;results.add(executor.submit(()->{try{place("alice","BUY","6000","1",key);return true;}catch(com.lumix.api.error.ApiException failure){assertEquals(com.lumix.api.error.ApiErrorCode.INSUFFICIENT_USER_BALANCE,failure.getErrorCode());return false;}}));}
            assertEquals(1,results.stream().map(f->{try{return f.get(15,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}}).filter(Boolean::booleanValue).count());
        } finally {executor.shutdownNow();}
        assertEquals(1,count("orders"));balance("alice","USDT","10000","6012");
        String id=jdbc.queryForObject("SELECT order_id FROM orders",String.class);
        assertThrows(NoSuchElementException.class,()->transaction.execute(status->engine.cancel("bob",id,"steal")));
        balance("alice","USDT","10000","6012");
    }

    /** 政策版本不可變、開單期間不得換規格，且治理改動與 audit 同交易提交。 */
    @Test void policyPublishingCannotRewriteRulesBeneathOpenOrders() throws Exception {
        var repository=new TradingPolicyRepository(jdbc,json);
        var current=repository.version("SPOT:BTCUSDT",1);
        var access=org.mockito.Mockito.mock(com.lumix.admin.superadmin.SuperAdminAccessService.class);
        var admin=new TradingPolicyAdministrationService(jdbc,new TradingExecutionSupport(jdbc,json,ledger),repository,access);
        var actor=new com.lumix.user.auth.domain.AuthenticatedUser("alice","alice@example.test","Alice");
        transaction.execute(status->admin.publishSpot(actor,current));
        var tree=json.valueToTree(current);((com.fasterxml.jackson.databind.node.ObjectNode)tree.get("rules")).put("policyVersion",2);
        var next=json.treeToValue(tree,TradingExecutionPolicy.class);
        var order=place("alice","BUY","100","0.1","before-policy-change");
        assertThrows(IllegalStateException.class,()->transaction.execute(status->admin.publishSpot(actor,next)));
        assertEquals(1,repository.active("spot","BTC-USDT").rules().policyVersion());
        transaction.execute(status->engine.cancel("alice",order.orderId(),"cancel-before-policy-change"));
        transaction.execute(status->admin.publishSpot(actor,next));
        assertEquals(2,repository.active("spot","BTC-USDT").rules().policyVersion());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action_type='TRADING_POLICY_PUBLISHED' AND actor_type='ADMIN'",Integer.class));
        org.mockito.Mockito.verify(access,org.mockito.Mockito.atLeastOnce()).requireActiveSuperAdmin(actor);
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE trading_execution_policies SET created_by='changed' WHERE instrument_id='SPOT:BTCUSDT'"));
    }

    /** 價格優先，再以持久化序號排序；累計成交與費用取整必須釋放買方多預留額。 */
    @Test void matchingUsesPriceThenPersistedSequenceAcrossSeveralMakers() {
        place("bob","SELL","100","0.1","expensive-first");
        var older=place("carol","SELL","99","0.1","best-older");
        var newer=place("bob","SELL","99","0.1","best-newer");
        place("alice","BUY","100","0.25","multi-match");
        var makers=jdbc.queryForList("SELECT t.maker_order_id FROM trades t JOIN trading_trade_executions e ON e.trade_id=t.trade_id ORDER BY e.ledger_journal_id",String.class);
        assertEquals(3,makers.size());assertEquals(older.orderId(),makers.get(0));assertEquals(newer.orderId(),makers.get(1));
        balance("alice","USDT","9975.1504","0");balance("alice","BTC","1.25","0");
        conserved("USDT");conserved("BTC");
    }

    private SpotTradingExecutionService engine(){return new SpotTradingExecutionService(jdbc,new TradingPolicyRepository(jdbc,json),new TradingExecutionSupport(jdbc,json,ledger),new ReservationHoldService(jdbc),new OrderReservationChangeService(jdbc,json),new TradingOrderQueryService(jdbc));}
    private TradingOrderQueryService.OrderView place(String owner,String side,String price,String quantity,String key){return transaction.execute(status->engine.place(owner,new SpotTradingExecutionService.Command("BTC-USDT",side,"LIMIT","GTC",price,quantity),key));}
    private long count(String table){return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class));}
    private void balance(String account,String asset,String total,String locked){var row=jdbc.queryForMap("SELECT total_amount,available_amount,locked_amount FROM balance_projections WHERE account_id=? AND asset_symbol=?",account,asset);assertMoney(total,(BigDecimal)row.get("total_amount"));assertMoney(locked,(BigDecimal)row.get("locked_amount"));assertMoney(new BigDecimal(total).subtract(new BigDecimal(locked)).toPlainString(),(BigDecimal)row.get("available_amount"));}
    private static void assertMoney(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    private void conserved(String asset){assertMoney("0",jdbc.queryForObject("SELECT SUM(CASE WHEN direction='CREDIT' THEN amount ELSE -amount END) FROM ledger_entries WHERE asset_symbol=?",BigDecimal.class,asset));}
    private void seed(String owner,String asset,String amount){String id="seed-"+owner+"-"+asset;ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId(id),new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT,id,List.of(new LedgerEntryDraft(new AccountId("treasury"),new AssetSymbol(asset),LedgerDirection.DEBIT,new BigDecimal(amount),1),new LedgerEntryDraft(new AccountId(owner),new AssetSymbol(asset),LedgerDirection.CREDIT,new BigDecimal(amount),2))),Instant.now()),id,new LedgerPostingActor("SYSTEM","test")));}
}
