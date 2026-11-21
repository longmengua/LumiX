package com.lumix.trading.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.*;
import com.lumix.ledger.runtime.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 同一交易中的冪等、鎖、帳本及稽核工具；不提供 HTTP 或獨立資金命令。 */
@Component
public class TradingExecutionSupport {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionalLedgerPostingService ledger;

    public TradingExecutionSupport(JdbcTemplate jdbc, ObjectMapper json, TransactionalLedgerPostingService ledger) {
        this.jdbc=jdbc; this.json=json; this.ledger=ledger;
    }

    /** 交易 runtime 先以全域 transaction lock 建立確定順序，再由資料列鎖保護跨模組資金。 */
    public void lockExecution() {
        jdbc.execute("SET LOCAL lock_timeout='5s'");
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(73621194001)", Object.class);
    }

    /** owner 來自認證 principal；凍結、關閉或非本人帳戶都不能成為新單資金來源。 */
    public String activeAccount(String owner, String kind) {
        String type="spot".equals(kind)?"SPOT":"futures".equals(kind)?"FUTURES":null;
        if(type==null)throw new IllegalArgumentException("unsupported trading kind");
        List<String> rows=jdbc.query("SELECT a.account_id FROM accounts a JOIN users u ON u.user_id=a.user_id "
                + "WHERE a.user_id=? AND a.account_type=? AND a.account_category='USER' AND a.status='ACTIVE' AND u.status='ACTIVE'",(r,n)->r.getString(1),owner,type);
        if(rows.size()!=1)throw new TradingOrderRejectedException("active owner trading account is required");
        return rows.getFirst();
    }

    /** 以穩定順序鎖住本次所有可能受影響的帳戶及既有 projection；新收款資產仍由 ledger 建立投影。 */
    public void lockAccounts(Collection<String> accounts) {
        for(String account:new TreeSet<>(accounts)) {
            List<String> rows=jdbc.query("SELECT a.status FROM accounts a JOIN users u ON u.user_id=a.user_id "
                    + "WHERE a.account_id=? AND u.status='ACTIVE' AND a.status='ACTIVE' FOR UPDATE OF a,u",(r,n)->r.getString(1),account);
            if(rows.size()!=1)throw new TradingOrderRejectedException("counterparty account is unavailable");
            jdbc.query("SELECT asset_symbol FROM balance_projections WHERE account_id=? ORDER BY asset_symbol FOR UPDATE",(r,n)->r.getString(1),account);
        }
    }

    /** 只建立經驗證的資產容器；不製造 ledger entry、餘額或固定可用額。 */
    public void requireAsset(String account,String asset) {
        Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM assets WHERE asset_symbol=? AND status='ACTIVE'",Integer.class,asset);
        if(active==null||active!=1)throw new TradingOrderRejectedException("asset is unavailable");
        jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,?,'ACTIVE') ON CONFLICT(account_id,asset_symbol) DO NOTHING",account,asset);
        Integer related=jdbc.queryForObject("SELECT COUNT(*) FROM account_assets WHERE account_id=? AND asset_symbol=? AND status='ACTIVE'",Integer.class,account,asset);
        if(related==null||related!=1)throw new TradingOrderRejectedException("account asset is unavailable");
    }

    /** 交易費用只可進入具名 EXCHANGE 收費帳戶；不從任何使用者帳戶借作費用對應帳戶。 */
    public void requireFeeAccount(String account,String asset) {
        Integer found=jdbc.queryForObject("SELECT COUNT(*) FROM accounts a JOIN account_assets aa ON aa.account_id=a.account_id "
                + "WHERE a.account_id=? AND a.account_category='EXCHANGE' AND a.account_purpose='TRADING_FEE_REVENUE' "
                + "AND a.status='ACTIVE' AND aa.asset_symbol=? AND aa.status='ACTIVE'",Integer.class,account,asset);
        if(found==null||found!=1)throw new IllegalStateException("governed fee account is unavailable");
    }

    /** durable owner-scoped key；追蹤 ID 不改變業務 fingerprint，重送定位原始委託並讀取最新狀態。 */
    public String claim(String scope,String owner,String key,String fingerprint,String request) {
        if(key==null||key.isBlank()||key.length()>128||key.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("idempotency key is required");
        String durable="trading:"+hash(encode(List.of(owner,key)));
        if(jdbc.update("INSERT INTO idempotency_keys(scope,idempotency_key,request_id,status,response_summary) VALUES (?,?,?,'IN_PROGRESS',?) "
                + "ON CONFLICT(scope,idempotency_key) DO NOTHING",scope,durable,request,fingerprint)==1)return null;
        Map<String,Object> previous=jdbc.queryForMap("SELECT status,response_summary,resource_id FROM idempotency_keys WHERE scope=? AND idempotency_key=?",scope,durable);
        if(!fingerprint.equals(previous.get("response_summary"))||!"COMPLETED".equals(previous.get("status")))throw new IllegalArgumentException("idempotency key conflicts with another command");
        return previous.get("resource_id").toString();
    }

    /** 與資金及委託狀態同 transaction 固定命令結果。 */
    public void complete(String scope,String owner,String key,String order) {
        String durable="trading:"+hash(encode(List.of(owner,key)));
        if(jdbc.update("UPDATE idempotency_keys SET status='COMPLETED',resource_type='ORDER',resource_id=?,updated_at=CURRENT_TIMESTAMP "
                + "WHERE scope=? AND idempotency_key=? AND status='IN_PROGRESS'",order,scope,durable)!=1)throw new IllegalStateException("order command result was not persisted");
    }

    /** ledger 仍負責每資產守恆與 projection 更新；呼叫端先完成授權及資金保留。 */
    public long post(String reference,String request,List<LedgerEntryDraft> entries) {
        return ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId(request),
                new LedgerJournalDraft(LedgerBusinessReferenceType.TRADE,reference,entries),Instant.now()),"trading-ledger:"+hash(reference),
                new LedgerPostingActor("SYSTEM","trading-execution"))).ledgerJournalId();
    }

    /** 零費用不建立無意義的零分錄；實際金額會再次受 ledger precision 驗證。 */
    public void entry(List<LedgerEntryDraft> entries,String account,String asset,LedgerDirection direction,BigDecimal amount) {
        if(amount.signum()==0)return;
        // PostgreSQL NUMERIC 固定 scale 並不表示業務多出小數；只去尾零，不對本金做取整。
        amount=amount.stripTrailingZeros();
        if(amount.scale()<0)amount=amount.setScale(0);
        TradingDecimal.ledgerAmount(amount);
        entries.add(new LedgerEntryDraft(new AccountId(account),new AssetSymbol(asset),direction,amount,entries.size()+1));
    }

    /** audit 與 outbox 是同交易證據，不能先回成功再另行補寫。 */
    public void evidence(String owner,String action,String order,String request,Object detail) {
        String value=encode(detail);
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('USER',?,?,'ORDER',?,?,'SUCCESS',?)",owner,action,order,request,value);
        jdbc.update("INSERT INTO outbox_events(aggregate_type,aggregate_id,event_type,payload,status,request_id) VALUES ('ORDER',?,?,?,'READY',?)",order,action,value,request);
    }

    /** canonical JSON 消除任意分隔符造成的內容碰撞。 */
    public String encode(Object value) { try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("cannot encode trading command",e);} }
    /** 摘要只用於內容識別及內部 key，不替代任何 owner 或資金驗證。 */
    public static String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
}
