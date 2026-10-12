package com.lumix.withdrawal.signing;

import com.lumix.withdrawal.address.EthereumWithdrawalAddress;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 將 final-risk 已通過的提款固定成一次性 signing intent。
 *
 * <p>intent 將鏈、資產、合約、來源／目的地址、原子金額與 expiry 編入 digest；沒有受控 Sepolia source
 * address 時直接 RISK_HOLD。nonce 與 EIP-1559 fee 由後續 serialized nonce/broadcast worker 補入前不得送 signer。</p>
 */
@Service
public class WithdrawalSigningIntentService {
    private final JdbcTemplate jdbc; private final WithdrawalPlatformProperties properties; private final WithdrawalStateMachine states=new WithdrawalStateMachine(); private final Clock clock;
    public WithdrawalSigningIntentService(JdbcTemplate jdbc,WithdrawalPlatformProperties properties){this(jdbc,properties,Clock.systemUTC());}
    WithdrawalSigningIntentService(JdbcTemplate jdbc,WithdrawalPlatformProperties properties,Clock clock){this.jdbc=Objects.requireNonNull(jdbc);this.properties=Objects.requireNonNull(properties);this.clock=Objects.requireNonNull(clock);}
    @Transactional public Intent create(String withdrawalId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT withdrawal_id,request_id,asset_symbol,chain_id,token_contract,address,amount,fee_amount,status FROM withdrawals WHERE withdrawal_id=? FOR UPDATE",withdrawalId);
        if(rows.size()!=1) throw new IllegalArgumentException("withdrawal does not exist"); Map<String,Object> w=rows.getFirst();
        if(!"SIGN_PENDING".equals(w.get("status"))) throw new IllegalStateException("withdrawal is not ready for signing");
        String source; try{source=EthereumWithdrawalAddress.normalize(properties.getSepoliaSourceAddress());}catch(RuntimeException exception){hold(withdrawalId,w,"configured Sepolia source address missing or invalid");throw new IllegalStateException("signing source is unavailable",exception);}
        long chainId=((Number)w.get("chain_id")).longValue(); if(chainId!=11155111L) { hold(withdrawalId,w,"only Sepolia signing is enabled"); throw new IllegalStateException("mainnet signing is disabled"); }
        String asset=(String)w.get("asset_symbol"); int decimals="USDT".equals(asset)?6:18; BigDecimal amount=(BigDecimal)w.get("amount"); String atomic=amount.movePointRight(decimals).setScale(0, RoundingMode.UNNECESSARY).toBigIntegerExact().toString(); Instant expires=Instant.now(clock).plus(properties.getSigningIntentTtl()); String to=EthereumWithdrawalAddress.normalize((String)w.get("address"));
        String canonical=String.join("\n",withdrawalId,Long.toString(chainId),asset,String.valueOf(w.get("token_contract")),source,to,atomic,expires.toString()); String hash=sha256(canonical);
        String payload="{\"withdrawalId\":\""+withdrawalId+"\",\"chainId\":"+chainId+",\"asset\":\""+asset+"\",\"tokenContract\":"+json(w.get("token_contract"))+",\"from\":\""+source+"\",\"to\":\""+to+"\",\"amountAtomic\":\""+atomic+"\",\"expiresAt\":\""+expires+"\"}";
        if(jdbc.update("INSERT INTO withdrawal_signing_requests(id,withdrawal_id,intent_hash,payload_json,status,expires_at) VALUES (?,?,?,CAST(? AS jsonb),'PENDING',?) ON CONFLICT(withdrawal_id) DO NOTHING",UUID.randomUUID(),withdrawalId,hash,payload,java.sql.Timestamp.from(expires))==0) return replay(withdrawalId);
        states.requireTransition(WithdrawalStatus.SIGN_PENDING,WithdrawalStatus.SIGNING); if(jdbc.update("UPDATE withdrawals SET status='SIGNING',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='SIGN_PENDING'",withdrawalId)!=1) throw new IllegalStateException("withdrawal signing transition lost concurrency race");
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('SYSTEM','withdrawal-signing-intent','WITHDRAWAL_SIGN_REQUESTED','WITHDRAWAL',? ,?,'SUCCESS','SIGNING')",withdrawalId,w.get("request_id"));
        return new Intent(withdrawalId,hash,source,to,asset,(String)w.get("token_contract"),atomic,expires,false);
    }
    private Intent replay(String withdrawalId){List<Intent> r=jdbc.query("SELECT intent_hash,payload_json,expires_at FROM withdrawal_signing_requests WHERE withdrawal_id=?",(rs,n)->new Intent(withdrawalId,rs.getString(1),null,null,null,null,null,rs.getTimestamp(3).toInstant(),true),withdrawalId); if(r.size()!=1)throw new IllegalStateException("signing intent idempotency conflict"); return r.getFirst();}
    private void hold(String id,Map<String,Object> row,String reason){states.requireTransition(WithdrawalStatus.SIGN_PENDING,WithdrawalStatus.RISK_HOLD);jdbc.update("UPDATE withdrawals SET status='RISK_HOLD',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='SIGN_PENDING'",id);jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,reason,after_state) VALUES ('SYSTEM','withdrawal-signing-intent','WITHDRAWAL_FINAL_RISK_REJECTED','WITHDRAWAL',? ,?,'REJECTED',?,'RISK_HOLD')",id,row.get("request_id"),reason);}
    private static String sha256(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}}
    private static String json(Object value){return value==null?"null":"\""+value.toString().replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    public record Intent(String withdrawalId,String intentHash,String from,String to,String asset,String tokenContract,String amountAtomic,Instant expiresAt,boolean replayed) { }
}
