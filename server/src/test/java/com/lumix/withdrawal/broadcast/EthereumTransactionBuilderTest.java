package com.lumix.withdrawal.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import org.junit.jupiter.api.Test;

/** 驗證交易建構固定 Sepolia、destination、value 與 USDT transfer ABI，避免簽錯交易。 */
class EthereumTransactionBuilderTest {
    private static final String USDT="0x3333333333333333333333333333333333333333";
    private final EthereumTransactionBuilder builder=builder();
    private static final String SOURCE="0x1111111111111111111111111111111111111111";
    private static final String DESTINATION="0x2222222222222222222222222222222222222222";
    @Test void ethBuildsEip1559DraftWithExactAtomicValue(){EthereumTransactionDraft draft=builder.build(11155111L,SOURCE,DESTINATION,"ETH",null,new BigDecimal("1.25"),BigInteger.valueOf(7),BigInteger.valueOf(21_000),BigInteger.valueOf(2_000_000_000L),BigInteger.valueOf(1_000_000_000L));assertEquals(new BigInteger("1250000000000000000"),draft.value());assertEquals(BigInteger.valueOf(7),draft.nonce());assertEquals(DESTINATION.toLowerCase(),draft.to());assertEquals("0x",draft.data());}
    @Test void usdtUsesErc20TransferAndSixDecimals(){EthereumTransactionDraft draft=builder.build(11155111L,SOURCE,DESTINATION,"USDT",USDT,new BigDecimal("12.345678"),BigInteger.ONE,BigInteger.valueOf(65_000),BigInteger.valueOf(2_000_000_000L),BigInteger.valueOf(1_000_000_000L));assertEquals(BigInteger.ZERO,draft.value());assertEquals(new BigInteger("12345678"),draft.amountAtomic());assertEquals("0xa9059cbb",draft.data().substring(0,10));assertTrueEndsWith(draft.data(),"0000000000000000000000000000000000000000000000000000000000bc614e");}
    @Test void arbitraryUsdtContractIsRejected(){assertThrows(IllegalArgumentException.class,()->builder.build(11155111L,SOURCE,DESTINATION,"USDT","0x4444444444444444444444444444444444444444",BigDecimal.ONE,BigInteger.ONE,BigInteger.valueOf(65_000),BigInteger.valueOf(2_000_000_000L),BigInteger.valueOf(1_000_000_000L)));}
    @Test void mainnetCannotConstructTransaction(){assertThrows(IllegalStateException.class,()->builder.build(1L,SOURCE,DESTINATION,"ETH",null,BigDecimal.ONE,BigInteger.ZERO,BigInteger.valueOf(21_000),BigInteger.ONE,BigInteger.ZERO));}
    private static void assertTrueEndsWith(String value,String suffix){if(!value.endsWith(suffix))throw new AssertionError("unexpected ABI data: "+value);}
    private static EthereumTransactionBuilder builder(){WithdrawalPlatformProperties properties=new WithdrawalPlatformProperties();properties.setSepoliaUsdtContract(USDT);return new EthereumTransactionBuilder(properties);}
}
