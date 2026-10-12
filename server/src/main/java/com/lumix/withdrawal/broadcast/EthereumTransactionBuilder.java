package com.lumix.withdrawal.broadcast;

import com.lumix.withdrawal.address.EthereumWithdrawalAddress;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

/** ETH 與白名單 USDT 的 unsigned EIP-1559 建構器；交易建構與 signer capability 保持分離。 */
@Service
public class EthereumTransactionBuilder {
    private final WithdrawalPlatformProperties properties;
    public EthereumTransactionBuilder(WithdrawalPlatformProperties properties) { this.properties=properties; }
    public EthereumTransactionDraft build(long chainId,String source,String destination,String asset,String tokenContract,BigDecimal amount,BigInteger nonce,BigInteger gasLimit,BigInteger maxFeePerGas,BigInteger maxPriorityFeePerGas) {
        if(chainId!=11155111L) throw new IllegalStateException("only Sepolia transaction construction is enabled");
        String from=EthereumWithdrawalAddress.normalize(source), to=EthereumWithdrawalAddress.normalize(destination); if(nonce.signum()<0||gasLimit.signum()<=0||maxFeePerGas.signum()<=0||maxPriorityFeePerGas.signum()<0||maxPriorityFeePerGas.compareTo(maxFeePerGas)>0) throw new IllegalArgumentException("invalid EIP-1559 fee or nonce");
        if("ETH".equals(asset)) { BigInteger atomic=atomic(amount,18); return new EthereumTransactionDraft(chainId,from,to,atomic,atomic,nonce,gasLimit,maxFeePerGas,maxPriorityFeePerGas,"0x",asset,null); }
        if("USDT".equals(asset)) { String contract=EthereumWithdrawalAddress.normalize(tokenContract); String configured=EthereumWithdrawalAddress.normalize(properties.getSepoliaUsdtContract()); if(!contract.equals(configured)) throw new IllegalArgumentException("unapproved Sepolia USDT contract"); BigInteger atomic=atomic(amount,6); String data=FunctionEncoder.encode(new Function("transfer",List.of(new Address(to),new Uint256(atomic)),List.of())); return new EthereumTransactionDraft(chainId,from,contract,BigInteger.ZERO,atomic,nonce,gasLimit,maxFeePerGas,maxPriorityFeePerGas,data,asset,contract); }
        throw new IllegalArgumentException("unsupported Ethereum withdrawal asset");
    }
    private static BigInteger atomic(BigDecimal amount,int decimals){return amount.movePointRight(decimals).setScale(0, RoundingMode.UNNECESSARY).toBigIntegerExact();}
}
