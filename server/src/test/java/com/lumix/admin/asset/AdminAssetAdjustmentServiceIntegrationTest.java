package com.lumix.admin.asset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.runtime.LedgerBalanceProjectionUpdater;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.testing.JdbcDataSource;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 真實 PostgreSQL 驗證 adjustment command 的 ledger、projection、audit 與 durable aggregate 同步提交。 */
class AdminAssetAdjustmentServiceIntegrationTest {
 @Test void manualCreditAndDebitPersistLedgerProjectionAndReplay() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  AssetAdjustmentResult credit=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.MANUAL_CORRECTION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,new BigDecimal("50"),null,null,null,null,null,"manual","key-credit"));
  assertFalse(credit.replayed()); assertEquals(0,new BigDecimal("150").compareTo(new BigDecimal(f.balance()))); assertEquals(1L,f.count("admin_asset_adjustments"));
  assertEquals(2L,f.count("ledger_entries WHERE ledger_journal_id="+credit.ledgerJournalId()));
  assertEquals("DEBIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='system:asset-adjustment:spot'",String.class,credit.ledgerJournalId()));
  assertEquals("CREDIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='user:spot'",String.class,credit.ledgerJournalId()));
  assertEquals(1L,f.count("audit_logs WHERE action_type='ADMIN_ASSET_ADJUSTMENT'"));
  assertTrue(f.run(new AssetAdjustmentCommand(AssetAdjustmentType.MANUAL_CORRECTION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,new BigDecimal("50"),null,null,null,null,null,"manual","key-credit")).replayed());
  assertEquals(1L,f.count("admin_asset_adjustments"));
  f.run(new AssetAdjustmentCommand(AssetAdjustmentType.MANUAL_CORRECTION,"user","SPOT","USDT",AssetAdjustmentDirection.DEBIT,new BigDecimal("40"),"CASE","1",null,null,"INC","debit","key-debit"));
  assertEquals(0,new BigDecimal("110").compareTo(new BigDecimal(f.balance()))); assertEquals(0L,f.count("balance_projections WHERE account_id='system:asset-adjustment:spot'"));
 }
 @Test void validatesAmountReferenceAndInsufficientDebitWithoutPartialEvidence() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("50")); long journals=f.count("ledger_journals");
  assertThrows(AssetAdjustmentException.class,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.MANUAL_CORRECTION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,BigDecimal.ZERO,null,null,null,null,null,"x","zero")));
  assertThrows(AssetAdjustmentException.class,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.COMPENSATION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,BigDecimal.ONE,"WITHDRAWAL",null,null,null,null,"x","half")));
  assertThrows(AssetAdjustmentException.class,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.COMPENSATION,"user","SPOT","USDT",AssetAdjustmentDirection.DEBIT,new BigDecimal("100"),null,null,null,null,null,"x","low")));
  assertEquals(0,new BigDecimal("50").compareTo(new BigDecimal(f.balance()))); assertEquals(journals,f.count("ledger_journals")); assertEquals(0L,f.count("admin_asset_adjustments"));
 }
 @Test void compensationPreservesOptionalReferencesAndPostsBalancedEffect() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  AssetAdjustmentResult result=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.COMPENSATION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,new BigDecimal("100"),"WITHDRAWAL","WD-TEST",null,null,"INC-1","compensation","comp-key"));
  assertEquals(0,new BigDecimal("200").compareTo(new BigDecimal(f.balance())));
  assertEquals("WITHDRAWAL",f.jdbc.queryForObject("SELECT source_business_type FROM admin_asset_adjustments WHERE adjustment_id=?",String.class,result.adjustmentId()));
  assertEquals("WD-TEST",f.jdbc.queryForObject("SELECT source_business_id FROM admin_asset_adjustments WHERE adjustment_id=?",String.class,result.adjustmentId()));
  assertEquals("DEBIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='system:asset-adjustment:spot'",String.class,result.ledgerJournalId()));
 }
 @Test void compensationAllowsNoBusinessOrIncidentOnlyAndDebit() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  AssetAdjustmentResult noReference=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.COMPENSATION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,new BigDecimal("10"),null,null,null,null,null,"no source","comp-none"));
  assertNull(f.jdbc.queryForObject("SELECT source_business_type FROM admin_asset_adjustments WHERE adjustment_id=?",String.class,noReference.adjustmentId()));
  AssetAdjustmentResult incident=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.COMPENSATION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,new BigDecimal("10"),null,null,null,null,"INC-2","incident","comp-incident"));
  assertEquals("INC-2",f.jdbc.queryForObject("SELECT incident_reference FROM admin_asset_adjustments WHERE adjustment_id=?",String.class,incident.adjustmentId()));
  AssetAdjustmentResult debit=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.COMPENSATION,"user","SPOT","USDT",AssetAdjustmentDirection.DEBIT,new BigDecimal("40"),null,null,null,null,null,"debit","comp-debit"));
  assertEquals(0,new BigDecimal("80").compareTo(new BigDecimal(f.balance())));
  assertEquals("CREDIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='system:asset-adjustment:spot'",String.class,debit.ledgerJournalId()));
 }
 @Test void businessReversalOfUserCreditDerivesDebitAndKeepsOriginalImmutable() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  String before=f.jdbc.queryForObject("SELECT direction||':'||amount::text||':'||account_id FROM ledger_entries WHERE ledger_entry_id=1",String.class);
  AssetAdjustmentResult first=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("30"),null,null,null,1L,null,"reverse","rev-1"));
  assertEquals(0,new BigDecimal("70").compareTo(new BigDecimal(f.balance())));
  assertEquals("DEBIT",f.jdbc.queryForObject("SELECT direction FROM admin_asset_adjustments WHERE adjustment_id=?",String.class,first.adjustmentId()));
  assertEquals(1L,f.jdbc.queryForObject("SELECT source_ledger_entry_id FROM admin_asset_adjustments WHERE adjustment_id=?",Long.class,first.adjustmentId()));
  assertEquals("CREDIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='system:asset-adjustment:spot'",String.class,first.ledgerJournalId()));
  f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("20"),null,null,null,1L,null,"reverse","rev-2"));
  assertEquals(0,new BigDecimal("50").compareTo(f.jdbc.queryForObject("SELECT 100-SUM(amount) FROM admin_asset_adjustments WHERE source_ledger_entry_id=1",BigDecimal.class)));
  assertEquals(before,f.jdbc.queryForObject("SELECT direction||':'||amount::text||':'||account_id FROM ledger_entries WHERE ledger_entry_id=1",String.class));
  f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("50"),null,null,null,1L,null,"reverse","rev-3"));
  assertThrows(AssetAdjustmentException.class,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,BigDecimal.ONE,null,null,null,1L,null,"reverse","rev-4")));
 }
 @Test void businessReversalOfUserDebitDerivesCredit() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  f.jdbc.update("INSERT INTO ledger_journals(business_reference_type,business_reference_id) VALUES('WITHDRAWAL','debit-source')");
  f.jdbc.update("INSERT INTO ledger_entries(ledger_journal_id,entry_sequence,account_id,asset_symbol,direction,amount) VALUES(2,1,'user:spot','USDT','DEBIT',100)");
  AssetAdjustmentResult result=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("30"),null,null,null,2L,null,"reverse debit","debit-rev"));
  assertEquals("CREDIT",f.jdbc.queryForObject("SELECT direction FROM admin_asset_adjustments WHERE adjustment_id=?",String.class,result.adjustmentId()));
  assertEquals("DEBIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='system:asset-adjustment:spot'",String.class,result.ledgerJournalId()));
  assertEquals("CREDIT",f.jdbc.queryForObject("SELECT direction FROM ledger_entries WHERE ledger_journal_id=? AND account_id='user:spot'",String.class,result.ledgerJournalId()));
  assertEquals(0,new BigDecimal("30").compareTo(new BigDecimal(f.balance())));
 }
 @Test void businessReversalRejectsUnknownSystemAndAdjustmentSources() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100")); long journals=f.count("ledger_journals");
  assertCode(ApiErrorCode.SOURCE_LEDGER_ENTRY_NOT_FOUND,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,BigDecimal.ONE,null,null,null,999L,null,"missing","missing")));
  f.jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES('exchange','e@x','e','SUSPENDED')");
  f.jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category) VALUES('exchange:spot','exchange','SPOT','ACTIVE','EXCHANGE')");
  f.jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES('exchange:spot','USDT','ACTIVE')");
  f.jdbc.update("INSERT INTO ledger_journals(business_reference_type,business_reference_id) VALUES('WITHDRAWAL','exchange-source')");
  f.jdbc.update("INSERT INTO ledger_entries(ledger_journal_id,entry_sequence,account_id,asset_symbol,direction,amount) VALUES(2,1,'exchange:spot','USDT','CREDIT',10)");
  assertCode(ApiErrorCode.SOURCE_LEDGER_ENTRY_NOT_ELIGIBLE,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,BigDecimal.ONE,null,null,null,2L,null,"exchange","exchange")));
  AssetAdjustmentResult adjustment=f.run(new AssetAdjustmentCommand(AssetAdjustmentType.MANUAL_CORRECTION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,new BigDecimal("10"),null,null,null,null,null,"manual","manual-source"));
  Long userEntry=f.jdbc.queryForObject("SELECT ledger_entry_id FROM ledger_entries WHERE ledger_journal_id=? AND account_id='user:spot'",Long.class,adjustment.ledgerJournalId());
  assertCode(ApiErrorCode.SOURCE_LEDGER_ENTRY_NOT_ELIGIBLE,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,BigDecimal.ONE,null,null,null,userEntry,null,"recursive","recursive")));
  assertEquals(journals+2,f.count("ledger_journals"));
 }
 @Test void businessReversalRejectsAmountAboveRemainingWithoutEvidence() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("30"),null,null,null,1L,null,"first","over-1")); long journals=f.count("ledger_journals"); String balance=f.balance();
  assertCode(ApiErrorCode.REVERSAL_AMOUNT_EXCEEDS_REMAINING,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("80"),null,null,null,1L,null,"over","over-2")));
  assertEquals(0,new BigDecimal("30").compareTo(f.jdbc.queryForObject("SELECT SUM(amount) FROM admin_asset_adjustments WHERE source_ledger_entry_id=1",BigDecimal.class))); assertEquals(journals,f.count("ledger_journals")); assertEquals(balance,f.balance());
 }
 @Test void concurrentBusinessReversalsCannotExceedOriginalEffect() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100")); ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
  try { Future<Object> a=pool.submit(()->concurrent(f,ready,start,"concurrent-a")); Future<Object> b=pool.submit(()->concurrent(f,ready,start,"concurrent-b")); ready.await(); start.countDown(); Object ar=a.get(), br=b.get();
   long success=java.util.stream.Stream.of(ar,br).filter(AssetAdjustmentResult.class::isInstance).count(); assertEquals(1L,success);
   assertEquals(0,new BigDecimal("70").compareTo(f.jdbc.queryForObject("SELECT SUM(amount) FROM admin_asset_adjustments WHERE adjustment_type='BUSINESS_REVERSAL'",BigDecimal.class))); assertEquals(0,new BigDecimal("30").compareTo(new BigDecimal(f.balance()))); assertEquals(1L,f.count("admin_asset_adjustments WHERE adjustment_type='BUSINESS_REVERSAL'"));
  } finally { pool.shutdownNow(); }
 }
 @Test void businessReversalIdempotencyReplaysAndRejectsPayloadConflict() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100")); AssetAdjustmentCommand first=new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("30"),null,null,null,1L,null,"idem","reversal-key");
  AssetAdjustmentResult created=f.run(first); AssetAdjustmentResult replay=f.run(first); assertTrue(replay.replayed()); assertEquals(created.adjustmentId(),replay.adjustmentId()); assertEquals(1L,f.count("admin_asset_adjustments")); assertEquals(1L,f.count("ledger_journals WHERE business_reference_type='ADJUSTMENT'")); assertEquals(0,new BigDecimal("70").compareTo(new BigDecimal(f.balance())));
  assertCode(ApiErrorCode.IDEMPOTENCY_CONFLICT,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("40"),null,null,null,1L,null,"idem","reversal-key"))); assertEquals(0,new BigDecimal("30").compareTo(f.jdbc.queryForObject("SELECT SUM(amount) FROM admin_asset_adjustments",BigDecimal.class)));
 }
 @Test void systemAccountPurposeIsRequiredByItsMatchingCommand() throws Exception {
  Fixture f=new Fixture(); f.seed(new BigDecimal("100"));
  f.jdbc.update("UPDATE accounts SET account_purpose=NULL WHERE account_id='system:airdrop:spot'"); f.jdbc.update("UPDATE accounts SET account_purpose='AIRDROP_FUNDING' WHERE account_id='system:asset-adjustment:spot'");
  assertCode(ApiErrorCode.INVALID_ACCOUNT_PURPOSE,()->f.run(new AssetAdjustmentCommand(AssetAdjustmentType.MANUAL_CORRECTION,"user","SPOT","USDT",AssetAdjustmentDirection.CREDIT,BigDecimal.ONE,null,null,null,null,null,"wrong purpose","purpose-adjustment")));
}
 private static Object concurrent(Fixture f,CountDownLatch ready,CountDownLatch start,String key) { ready.countDown(); try { start.await(); return f.run(new AssetAdjustmentCommand(AssetAdjustmentType.BUSINESS_REVERSAL,null,null,null,null,new BigDecimal("70"),null,null,null,1L,null,"parallel",key)); } catch (Exception e) { return e; } }
 private static void assertCode(ApiErrorCode expected, org.junit.jupiter.api.function.Executable action) { assertEquals(expected,assertThrows(AssetAdjustmentException.class,action).getErrorCode()); }
 private static final class Fixture { final JdbcDataSource ds=new JdbcDataSource(); final JdbcTemplate jdbc; final TransactionTemplate tx; final AssetAdjustmentService service; final AuthenticatedUser actor=new AuthenticatedUser("admin","a@x","a");
  Fixture(){ds.setURL("jdbc:postgresql:test:asset_adjustment_it");ds.setUser("x");ds.setPassword("x"); Flyway fw=Flyway.configure().dataSource(ds).locations(System.getProperty("lumix.test.migration.location","classpath:db/migration")).cleanDisabled(false).load();fw.clean();fw.migrate();jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));SuperAdminAccessService access=mock(SuperAdminAccessService.class);doNothing().when(access).requireActiveSuperAdmin(actor);service=new AssetAdjustmentService(access,jdbc,new TransactionalLedgerPostingService(jdbc,new LedgerInvariantPolicy(),new LedgerBalanceProjectionUpdater(jdbc)));}
  void seed(BigDecimal amount)throws Exception{try(Connection c=ds.getConnection();var s=c.createStatement()){s.executeUpdate("INSERT INTO users(user_id,email,display_name,status) VALUES('admin','a@x','a','ACTIVE'),('user','u@x','u','ACTIVE')");s.executeUpdate("INSERT INTO accounts(account_id,user_id,account_type,status) VALUES('user:spot','user','SPOT','ACTIVE')");s.executeUpdate("INSERT INTO assets(asset_symbol,display_name,precision_scale,status) VALUES('USDT','USDT',6,'ACTIVE')");s.executeUpdate("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES('user:spot','USDT','ACTIVE')");s.executeUpdate("INSERT INTO ledger_journals(business_reference_type,business_reference_id) VALUES('WITHDRAWAL','seed')");s.executeUpdate("INSERT INTO ledger_entries(ledger_journal_id,entry_sequence,account_id,asset_symbol,direction,amount) VALUES(1,1,'user:spot','USDT','CREDIT',"+amount+")");s.executeUpdate("INSERT INTO balance_projections(account_id,asset_symbol,total_amount,available_amount,locked_amount,projection_version) VALUES('user:spot','USDT',"+amount+","+amount+",0,1)");}}
  AssetAdjustmentResult run(AssetAdjustmentCommand c){return tx.execute(x->service.execute(actor,c));} long count(String q){Long v=jdbc.queryForObject("SELECT count(*) FROM "+q,Long.class);return v==null?0:v;} String balance(){return jdbc.queryForObject("SELECT available_amount::text FROM balance_projections WHERE account_id='user:spot' AND asset_symbol='USDT'",String.class);}}
}
