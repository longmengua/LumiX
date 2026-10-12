package com.lumix.signer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.RawTransaction;

/**
 * 獨立 signer 的實際簽章 service。
 *
 * <p>其 durable H2 store 僅記錄 withdrawal／intent 與簽章結果，避免同一 intent 重複簽署；它不能連到
 * wallet server 的 PostgreSQL，也不做 broadcast。任何 policy mismatch 都在簽章前拒絕。</p>
 */
@Service
public class SignerService {
    private final JdbcTemplate jdbc; private final SigningKeyProvider keys; private final SignerProperties properties; private final Clock clock=Clock.systemUTC();
    public SignerService(JdbcTemplate jdbc,SigningKeyProvider keys,SignerProperties properties){this.jdbc=Objects.requireNonNull(jdbc);this.keys=Objects.requireNonNull(keys);this.properties=Objects.requireNonNull(properties); jdbc.execute("CREATE TABLE IF NOT EXISTS signer_requests (withdrawal_id VARCHAR(64) PRIMARY KEY,intent_hash CHAR(64) NOT NULL,asset VARCHAR(16),amount_atomic DECIMAL(78,0),transaction_hash CHAR(66) NOT NULL,signed_raw CLOB NOT NULL,created_at TIMESTAMP NOT NULL)"); jdbc.execute("ALTER TABLE signer_requests ADD COLUMN IF NOT EXISTS asset VARCHAR(16)"); jdbc.execute("ALTER TABLE signer_requests ADD COLUMN IF NOT EXISTS amount_atomic DECIMAL(78,0)"); jdbc.execute("CREATE TABLE IF NOT EXISTS signer_policy_locks (asset VARCHAR(16) PRIMARY KEY)"); jdbc.update("MERGE INTO signer_policy_locks(asset) KEY(asset) VALUES ('ETH')"); jdbc.update("MERGE INTO signer_policy_locks(asset) KEY(asset) VALUES ('USDT')");}
    @Transactional public SignerResult sign(SignerRequest request) {
        if(request==null) throw new IllegalArgumentException("signing request is required");
        var existing=existing(request.withdrawalId());
        if(!existing.isEmpty()) { if(!existing.getFirst()[0].equals(request.intentHash())) throw new IllegalStateException("withdrawal id cannot be signed with a different intent"); return new SignerResult(existing.getFirst()[1],existing.getFirst()[2],true); }
        // 同一 asset 的 signer policy row lock 序列化 daily aggregate 的讀取與新增，不能讓並發簽章穿透限額。
        lockPolicy(request.asset());
        String expected=canonicalHash(request); if(!expected.equals(request.intentHash())) throw new IllegalArgumentException("signing intent digest mismatch");
        validate(request);
        RawTransaction transaction=RawTransaction.createTransaction(request.chainId(),request.nonce(),request.gasLimit(),request.to(),request.amount(),request.data(),request.maxPriorityFeePerGas(),request.maxFeePerGas());
        String raw="0x"+HexFormat.of().formatHex(TransactionEncoder.signMessage(transaction,request.chainId(),keys.credentials())); String txHash=Hash.sha3(raw);
        jdbc.update("INSERT INTO signer_requests(withdrawal_id,intent_hash,asset,amount_atomic,transaction_hash,signed_raw,created_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",request.withdrawalId(),request.intentHash(),request.asset().toUpperCase(Locale.ROOT),request.amountAtomic(),txHash,raw);
        return new SignerResult(txHash,raw,false);
    }
    private java.util.List<String[]> existing(String withdrawalId) { return jdbc.query("SELECT intent_hash,transaction_hash,signed_raw FROM signer_requests WHERE withdrawal_id=?",(rs,n)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},withdrawalId); }
    private void lockPolicy(String asset) { if(blank(asset)) throw new IllegalArgumentException("asset is required"); if(jdbc.query("SELECT asset FROM signer_policy_locks WHERE asset=? FOR UPDATE",(rs,n)->rs.getString(1),asset.toUpperCase(Locale.ROOT)).size()!=1) throw new IllegalStateException("unsupported signer policy asset"); }
    /** 讓 wallet server 在 HTTP timeout 後先查 durable result，避免把不確定結果當成可重新簽章。 */
    public SignerResult result(String withdrawalId,String intentHash) {
        var rows=jdbc.query("SELECT intent_hash,transaction_hash,signed_raw FROM signer_requests WHERE withdrawal_id=?",(rs,n)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},withdrawalId);
        if(rows.isEmpty()) return null;
        if(!rows.getFirst()[0].equals(intentHash)) throw new IllegalStateException("withdrawal id belongs to a different signing intent");
        return new SignerResult(rows.getFirst()[1],rows.getFirst()[2],true);
    }
    /** signer 自行重算所有可簽欄位的 digest，不能僅信任 wallet server 提供 hash。 */
    static String canonicalHash(SignerRequest r) { String text=String.join("\n",r.withdrawalId(),Long.toString(r.chainId()),r.asset(),String.valueOf(r.tokenContract()),r.from(),r.to(),r.amount().toString(),r.amountAtomic().toString(),r.nonce().toString(),r.gasLimit().toString(),r.maxFeePerGas().toString(),r.maxPriorityFeePerGas().toString(),r.data(),r.expiresAt().toString()); try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);} }
    private void validate(SignerRequest r) { if(r==null||blank(r.withdrawalId())||blank(r.intentHash())||blank(r.asset())||blank(r.from())||blank(r.to())||r.amount()==null||r.amount().signum()<0||r.amountAtomic()==null||r.amountAtomic().signum()<=0||r.nonce()==null||r.gasLimit()==null||r.maxFeePerGas()==null||r.maxPriorityFeePerGas()==null||r.expiresAt()==null||!Instant.now(clock).isBefore(r.expiresAt())) throw new IllegalArgumentException("invalid or expired signing request"); String asset=r.asset().toUpperCase(Locale.ROOT); if(r.chainId()!=properties.getChainId()||r.chainId()!=11155111L||!properties.getAllowedAssets().contains(asset)||!r.from().equalsIgnoreCase(keys.credentials().getAddress())||properties.getSourceAddress()==null||!r.from().equalsIgnoreCase(properties.getSourceAddress())||r.amountAtomic().compareTo(limitAtomic(asset,properties.getSingleSigningLimit()))>0||dailyAmount(asset).add(r.amountAtomic()).compareTo(limitAtomic(asset,properties.getDailySigningLimit()))>0) throw new IllegalStateException("signer policy rejected request"); if("USDT".equals(asset)) validateUsdtTransfer(r); else if("ETH".equals(asset)) validateEthTransfer(r); else throw new IllegalStateException("unsupported asset"); }
    /** signer 自行驗證 ERC-20 calldata，不可只相信 wallet server 附帶的 amountAtomic 欄位。 */
    private void validateUsdtTransfer(SignerRequest r) { if(properties.getSepoliaUsdtContract()==null||!properties.getSepoliaUsdtContract().equalsIgnoreCase(r.tokenContract())||!r.to().equalsIgnoreCase(r.tokenContract())||r.amount().signum()!=0||r.data()==null||!r.data().startsWith("0xa9059cbb")||r.data().length()!=138) throw new IllegalStateException("unapproved USDT transfer payload"); try { java.math.BigInteger encoded=new java.math.BigInteger(r.data().substring(74),16); if(!encoded.equals(r.amountAtomic())) throw new IllegalStateException("USDT calldata amount mismatch"); } catch(NumberFormatException exception) { throw new IllegalStateException("invalid USDT calldata",exception); } }
    private void validateEthTransfer(SignerRequest r) { if(r.tokenContract()!=null||!"0x".equals(r.data())||!r.amount().equals(r.amountAtomic())) throw new IllegalStateException("invalid ETH transfer payload"); }
    /** limit 以人類資產單位設定，但 policy 比較永遠使用 transaction 中不可變的原子單位。 */
    private java.math.BigInteger limitAtomic(String asset,BigDecimal limit) { if(limit==null||limit.signum()<=0)return java.math.BigInteger.ZERO; return limit.movePointRight("USDT".equals(asset)?6:18).toBigIntegerExact(); }
    private java.math.BigInteger dailyAmount(String asset) { java.math.BigDecimal value=jdbc.queryForObject("SELECT COALESCE(sum(amount_atomic),0) FROM signer_requests WHERE asset=? AND created_at >= CURRENT_DATE",java.math.BigDecimal.class,asset); return value==null?java.math.BigInteger.ZERO:value.toBigIntegerExact(); }
    private static boolean blank(String s){return s==null||s.isBlank();}
}
