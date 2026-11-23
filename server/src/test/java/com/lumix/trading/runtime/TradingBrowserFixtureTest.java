package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.runtime.LedgerBalanceProjectionUpdater;
import com.lumix.ledger.runtime.LedgerPostingActor;
import com.lumix.ledger.runtime.LedgerPostingExecutionCommand;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 專用 lumix_e2e 資料庫的本機雙帳戶瀏覽器 fixture；不載入正式應用程式。 */
class TradingBrowserFixtureTest {
    @Test
    void provisionDedicatedBrowserDatabase() throws Exception {
        String url=System.getenv("LUMIX_E2E_JDBC_URL");
        assumeTrue(url!=null && url.equals("jdbc:postgresql://127.0.0.1:5432/lumix_e2e"));
        String password=System.getenv("LUMIX_E2E_POSTGRES_PASSWORD");
        assumeTrue(password!=null && !password.isBlank());
        var source=new DriverManagerDataSource(url,"lumix_app",password);
        Integer existing=new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM pg_tables "
                + "WHERE schemaname='public'",Integer.class);
        assumeTrue(existing!=null && existing==0,"browser fixture requires a new empty database");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(source);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.update("INSERT INTO assets(asset_symbol,display_name,precision_scale,status) VALUES "
                + "('BTC','Bitcoin',8,'ACTIVE'),('ETH','Ether',8,'ACTIVE'),('USDT','Tether',6,'ACTIVE')");
        jdbc.update("INSERT INTO markets(market_symbol,base_asset_symbol,quote_asset_symbol,price_scale,"
                + "quantity_scale,status) VALUES ('BTC-USDT','BTC','USDT',2,4,'ACTIVE'),"
                + "('ETH-USDT','ETH','USDT',2,4,'ACTIVE')");
        user(jdbc,"alice",true);user(jdbc,"bob",true);user(jdbc,"admin",true);
        user(jdbc,"treasury",false);user(jdbc,"fees",false);user(jdbc,"margin",false);
        user(jdbc,"insurance",false);user(jdbc,"conversion",false);
        account(jdbc,"alice-spot","alice","SPOT",true,null);
        account(jdbc,"alice-futures","alice","FUTURES",true,null);
        account(jdbc,"bob-spot","bob","SPOT",true,null);
        account(jdbc,"bob-futures","bob","FUTURES",true,null);
        account(jdbc,"admin-spot","admin","SPOT",true,null);
        account(jdbc,"admin-futures","admin","FUTURES",true,null);
        account(jdbc,"treasury","treasury","SPOT",false,null);
        account(jdbc,"fees","fees","FUTURES",false,"TRADING_FEE_REVENUE");
        account(jdbc,"margin","margin","FUTURES",false,"PERPETUAL_MARGIN_POOL");
        account(jdbc,"insurance","insurance","FUTURES",false,"PERPETUAL_INSURANCE");
        account(jdbc,"conversion","conversion","FUTURES",false,"PERPETUAL_CONVERSION_POOL");
        jdbc.update("INSERT INTO admin_principals(user_id,role,status,activated_at) "
                + "VALUES ('admin','SUPER_ADMIN','ACTIVE',CURRENT_TIMESTAMP)");
        var ledger=new TransactionalLedgerPostingService(jdbc,new LedgerInvariantPolicy(),
                new LedgerBalanceProjectionUpdater(jdbc));
        tx.executeWithoutResult(status->{
            for(String owner:List.of("alice","bob")){
                seed(ledger,owner+"-spot","USDT","100000");
                seed(ledger,owner+"-spot","BTC","2");
                seed(ledger,owner+"-futures","USDT","100000");
            }
            seed(ledger,"insurance","USDT","1000");
        });
        var cookies=new java.util.HashMap<String,String>();
        for(String owner:List.of("alice","bob","admin")){
            cookies.put(owner,session(jdbc,owner));
        }
        Path file=Path.of(System.getProperty("java.io.tmpdir"),"lumix-e2e-cookies.json");
        Files.writeString(file,new ObjectMapper().writeValueAsString(cookies),StandardCharsets.UTF_8);
        assertEquals(13,jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=true",Integer.class));
    }

    private static void user(JdbcTemplate jdbc,String id,boolean active){
        jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES (?,?,?,?)",
                id,id+"@example.test",id,active?"ACTIVE":"SUSPENDED");
    }
    private static void account(JdbcTemplate jdbc,String id,String owner,String kind,boolean person,String purpose){
        jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) "
                + "VALUES (?,?,?,'ACTIVE',?,?)",id,owner,kind,person?"USER":"EXCHANGE",purpose);
        for(String asset:List.of("USDT","BTC"))
            jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,?,'ACTIVE')",id,asset);
    }
    private static void seed(TransactionalLedgerPostingService ledger,String target,String asset,String quantity){
        String id="e2e_"+UUID.randomUUID();
        ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId(id),
                new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT,id,List.of(
                        new LedgerEntryDraft(new AccountId("treasury"),new AssetSymbol(asset),
                                LedgerDirection.DEBIT,new BigDecimal(quantity),1),
                        new LedgerEntryDraft(new AccountId(target),new AssetSymbol(asset),
                                LedgerDirection.CREDIT,new BigDecimal(quantity),2))),Instant.now()),
                id,new LedgerPostingActor("SYSTEM","e2e-fixture")));
    }
    private static String session(JdbcTemplate jdbc,String owner) throws Exception {
        UUID id=UUID.randomUUID();
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String secret=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8)));
        jdbc.update("INSERT INTO user_sessions(session_id,user_id,token_digest,expires_at) "
                + "VALUES (?,?,?,CURRENT_TIMESTAMP+INTERVAL '2 hours')",id,owner,digest);
        return id+"."+secret;
    }
}
