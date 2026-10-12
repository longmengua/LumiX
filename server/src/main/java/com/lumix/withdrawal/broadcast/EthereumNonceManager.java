package com.lumix.withdrawal.broadcast;

import com.lumix.withdrawal.address.EthereumWithdrawalAddress;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用主資料庫列鎖分配 nonce。
 *
 * <p>首次 allocation 必須由 caller 提供 provider 的 pending nonce；之後只能依持久游標遞增，防止多 pod
 * 以 `latest` 查詢得到相同 nonce。重送同 withdrawal 回傳原本 allocation。</p>
 */
@Service
public class EthereumNonceManager {
    private final JdbcTemplate jdbc;
    public EthereumNonceManager(JdbcTemplate jdbc){this.jdbc=Objects.requireNonNull(jdbc);}
    @Transactional public BigInteger allocate(String withdrawalId,long chainId,String sourceAddress,BigInteger providerPendingNonce) {
        String source=EthereumWithdrawalAddress.normalize(sourceAddress); if(chainId!=11155111L||providerPendingNonce==null||providerPendingNonce.signum()<0) throw new IllegalArgumentException("valid Sepolia pending nonce is required");
        List<BigInteger> existing=jdbc.query("SELECT nonce FROM withdrawal_nonce_allocations WHERE withdrawal_id=? FOR UPDATE",(rs,n)->rs.getBigDecimal(1).toBigIntegerExact(),withdrawalId); if(!existing.isEmpty()) return existing.getFirst();
        jdbc.update("INSERT INTO withdrawal_nonce_states(chain_id,source_address,next_nonce) VALUES (?,?,?) ON CONFLICT(chain_id,source_address) DO NOTHING",chainId,source,providerPendingNonce);
        List<Map<String,Object>> state=jdbc.queryForList("SELECT next_nonce FROM withdrawal_nonce_states WHERE chain_id=? AND source_address=? FOR UPDATE",chainId,source); if(state.size()!=1) throw new IllegalStateException("nonce state cannot be locked"); BigInteger stored=((BigDecimal)state.getFirst().get("next_nonce")).toBigIntegerExact();
        // 外部交易或前一個不確定 broadcast 可能已推進 pending nonce；絕不能倒退並重用舊 nonce。
        BigInteger nonce=stored.max(providerPendingNonce);
        if (!nonce.equals(stored)) jdbc.update("UPDATE withdrawal_nonce_states SET next_nonce=?,updated_at=CURRENT_TIMESTAMP WHERE chain_id=? AND source_address=?",nonce,chainId,source);
        jdbc.update("INSERT INTO withdrawal_nonce_allocations(withdrawal_id,chain_id,source_address,nonce) VALUES (?,?,?,?)",withdrawalId,chainId,source,nonce);
        if(jdbc.update("UPDATE withdrawal_nonce_states SET next_nonce=next_nonce+1,updated_at=CURRENT_TIMESTAMP WHERE chain_id=? AND source_address=? AND next_nonce=?",chainId,source,nonce)!=1) throw new IllegalStateException("nonce allocation concurrency failure"); return nonce;
    }
}
