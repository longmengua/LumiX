package com.lumix.withdrawal.signing;

import com.lumix.ethereum.rpc.EthereumRpcClient;
import com.lumix.withdrawal.broadcast.EthereumNonceManager;
import com.lumix.withdrawal.broadcast.EthereumTransactionBuilder;
import com.lumix.withdrawal.broadcast.EthereumTransactionDraft;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import com.lumix.withdrawal.application.WithdrawalMetrics;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;

/** 將 persisted intent 補齊 nonce/EIP-1559 policy 後送到隔離 signer；主 server 仍不持有 key。 */
@Service
@ConditionalOnBean({EthereumRpcClient.class, SignerGateway.class})
public class WithdrawalSigningExecutionService {
    private final JdbcTemplate jdbc; private final EthereumNonceManager nonces; private final EthereumTransactionBuilder builder; private final EthereumRpcClient rpc; private final SignerGateway signer; private final WithdrawalPlatformProperties properties; private final WithdrawalMetrics metrics; private final WithdrawalStateMachine states=new WithdrawalStateMachine();
    public WithdrawalSigningExecutionService(JdbcTemplate jdbc,EthereumNonceManager nonces,EthereumTransactionBuilder builder,EthereumRpcClient rpc,SignerGateway signer,WithdrawalPlatformProperties properties,WithdrawalMetrics metrics){this.jdbc=Objects.requireNonNull(jdbc);this.nonces=Objects.requireNonNull(nonces);this.builder=Objects.requireNonNull(builder);this.rpc=Objects.requireNonNull(rpc);this.signer=Objects.requireNonNull(signer);this.properties=Objects.requireNonNull(properties);this.metrics=Objects.requireNonNull(metrics);}
    @Transactional public void sign(String withdrawalId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT w.withdrawal_id,w.request_id,w.asset_symbol,w.address,w.amount,w.token_contract,w.chain_id,w.status,s.intent_hash,s.expires_at,s.status signing_status FROM withdrawals w JOIN withdrawal_signing_requests s ON s.withdrawal_id=w.withdrawal_id WHERE w.withdrawal_id=? FOR UPDATE",withdrawalId);
        if(rows.size()!=1) throw new IllegalArgumentException("signing withdrawal does not exist"); Map<String,Object> row=rows.getFirst(); if(!"SIGNING".equals(row.get("status"))||!"PENDING".equals(row.get("signing_status"))) return;
        Instant expires=((java.sql.Timestamp)row.get("expires_at")).toInstant(); if(!Instant.now().isBefore(expires)) { hold(row,"signing intent expired"); return; }
        String source=properties.getSepoliaSourceAddress(); long chain=((Number)row.get("chain_id")).longValue(); java.math.BigInteger nonce; try { nonce=nonces.allocate(withdrawalId,chain,source,rpc.pendingTransactionCount(source)); } catch(java.io.IOException exception) { metrics.rpcError(); throw new IllegalStateException("pending nonce cannot be observed safely",exception); }
        String asset=(String)row.get("asset_symbol"); EthereumTransactionDraft draft=builder.build(chain,source,(String)row.get("address"),asset,(String)row.get("token_contract"),(BigDecimal)row.get("amount"),nonce,"USDT".equals(asset)?properties.getErc20GasLimit():properties.getEthGasLimit(),properties.getMaxFeePerGas(),properties.getMaxPriorityFeePerGas());
        String hash=digest(withdrawalId,draft,expires); SignerGateway.SigningRequest request=new SignerGateway.SigningRequest(withdrawalId,hash,chain,asset,draft.tokenContract(),draft.from(),draft.to(),draft.value(),draft.amountAtomic(),draft.nonce(),draft.gasLimit(),draft.maxFeePerGas(),draft.maxPriorityFeePerGas(),draft.data(),expires);
        SignerGateway.SigningResult result;
        try { result=signer.sign(request); } catch(RuntimeException exception) {
            metrics.signerError();
            try { result=signer.result(withdrawalId,hash); }
            catch(RuntimeException replayException) {
                // signer durable record 與本地 intent 不一致時不可重簽或無限重試，必須交給人工風控處理。
                hold(row,"signer idempotency result conflicts with signing intent");
                return;
            }
            if(result==null) throw exception;
        }
        if(!hash.equals(request.intentHash())||result.transactionHash()==null||result.signedRawTransaction()==null) { hold(row,"ambiguous signing result"); return; }
        // txHash 必須由主服務對 signer 回傳的 raw transaction 本地推導；不得把 signer 自報 hash 當唯一廣播對帳證據。
        String derivedTransactionHash=Hash.sha3(result.signedRawTransaction());
        if(!derivedTransactionHash.equalsIgnoreCase(result.transactionHash())) { hold(row,"signer transaction hash does not match signed raw transaction"); return; }
        jdbc.update("UPDATE withdrawal_signing_requests SET intent_hash=?,payload_json=CAST(? AS jsonb),status='SIGNED',signed_raw_transaction=?,signed_transaction_hash=? WHERE withdrawal_id=? AND status='PENDING'",hash,payload(draft,expires),result.signedRawTransaction(),result.transactionHash(),withdrawalId);
        states.requireTransition(WithdrawalStatus.SIGNING,WithdrawalStatus.SIGNED); if(jdbc.update("UPDATE withdrawals SET status='SIGNED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='SIGNING'",withdrawalId)!=1) throw new IllegalStateException("signing state race");
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('SYSTEM','signer-gateway','WITHDRAWAL_SIGNED','WITHDRAWAL',? ,?,'SUCCESS','SIGNED')",withdrawalId,row.get("request_id")); metrics.signed();
    }
    private void hold(Map<String,Object> row,String reason){states.requireTransition(WithdrawalStatus.SIGNING,WithdrawalStatus.RISK_HOLD);jdbc.update("UPDATE withdrawals SET status='RISK_HOLD',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status='SIGNING'",row.get("withdrawal_id"));jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,reason,after_state) VALUES ('SYSTEM','signer-gateway','WITHDRAWAL_SIGN_REQUESTED','WITHDRAWAL',? ,?,'REJECTED',?,'RISK_HOLD')",row.get("withdrawal_id"),row.get("request_id"),reason);}
    /** 必須與 signer 模組 canonical field order 完全相同。 */
    static String digest(String withdrawalId,EthereumTransactionDraft d,Instant expires){String canonical=String.join("\n",withdrawalId,Long.toString(d.chainId()),d.asset(),String.valueOf(d.tokenContract()),d.from(),d.to(),d.value().toString(),d.amountAtomic().toString(),d.nonce().toString(),d.gasLimit().toString(),d.maxFeePerGas().toString(),d.maxPriorityFeePerGas().toString(),d.data(),expires.toString());try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}}
    private static String payload(EthereumTransactionDraft d,Instant expires){return "{\"chainId\":"+d.chainId()+",\"from\":\""+d.from()+"\",\"to\":\""+d.to()+"\",\"nonce\":\""+d.nonce()+"\",\"data\":\""+d.data()+"\",\"expiresAt\":\""+expires+"\"}";}
}
