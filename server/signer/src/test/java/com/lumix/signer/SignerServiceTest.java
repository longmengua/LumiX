package com.lumix.signer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

/**
 * signer 的額度與 idempotency 必須由其獨立 durable store 自行保證。
 *
 * <p>這裡的 deterministic key 僅是 test fixture，絕不屬於 dev 或 production 設定。</p>
 */
class SignerServiceTest {
    private static final String USDT = "0x3333333333333333333333333333333333333333";
    @Test void dailyLimitAndIntentReplayAreEnforcedBySignerStore() {
        Credentials credentials = Credentials.create(ECKeyPair.create(BigInteger.ONE));
        SignerProperties properties = new SignerProperties();
        properties.setSourceAddress(credentials.getAddress());
        properties.setSingleSigningLimit(new BigDecimal("0.6"));
        properties.setDailySigningLimit(new BigDecimal("0.5"));
        SignerService service = new SignerService(jdbc(), () -> credentials, properties);

        SignerRequest first = request("wd-one", credentials, new BigInteger("400000000000000000"));
        SignerResult firstResult = service.sign(first);
        assertTrue(!firstResult.replayed());
        assertTrue(service.sign(first).replayed());

        SignerRequest changedHash = new SignerRequest("wd-one", "f".repeat(64), first.chainId(), first.asset(), first.tokenContract(), first.from(), first.to(), first.amount(), first.amountAtomic(), first.nonce(), first.gasLimit(), first.maxFeePerGas(), first.maxPriorityFeePerGas(), first.data(), first.expiresAt());
        assertThrows(IllegalStateException.class, () -> service.sign(changedHash));

        SignerRequest overDaily = request("wd-two", credentials, new BigInteger("200000000000000000"));
        assertThrows(IllegalStateException.class, () -> service.sign(overDaily));
    }

    @Test void usdtCalldataAmountMustMatchImmutableAmount() {
        Credentials credentials = Credentials.create(ECKeyPair.create(BigInteger.ONE));
        SignerProperties properties = new SignerProperties();
        properties.setSourceAddress(credentials.getAddress());
        properties.setSingleSigningLimit(new BigDecimal("100"));
        properties.setDailySigningLimit(new BigDecimal("100"));
        properties.setSepoliaUsdtContract(USDT);
        SignerService service = new SignerService(jdbc(), () -> credentials, properties);
        String data = "0xa9059cbb" + "0".repeat(24) + "2222222222222222222222222222222222222222" + "0".repeat(63) + "2";
        SignerRequest unsigned = new SignerRequest("wd-usdt", "", 11155111L, "USDT", USDT, credentials.getAddress(), USDT, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.valueOf(65_000), BigInteger.valueOf(2_000_000_000L), BigInteger.valueOf(1_000_000_000L), data, Instant.now().plusSeconds(60));
        SignerRequest signed = new SignerRequest(unsigned.withdrawalId(), SignerService.canonicalHash(unsigned), unsigned.chainId(), unsigned.asset(), unsigned.tokenContract(), unsigned.from(), unsigned.to(), unsigned.amount(), unsigned.amountAtomic(), unsigned.nonce(), unsigned.gasLimit(), unsigned.maxFeePerGas(), unsigned.maxPriorityFeePerGas(), unsigned.data(), unsigned.expiresAt());
        assertThrows(IllegalStateException.class, () -> service.sign(signed));
        String validData = "0xa9059cbb" + "0".repeat(24) + "2222222222222222222222222222222222222222" + "0".repeat(63) + "1";
        SignerRequest validUnsigned = new SignerRequest("wd-usdt-valid", "", 11155111L, "USDT", USDT, credentials.getAddress(), USDT, BigInteger.ZERO, BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(65_000), BigInteger.valueOf(2_000_000_000L), BigInteger.valueOf(1_000_000_000L), validData, Instant.now().plusSeconds(60));
        SignerRequest valid = new SignerRequest(validUnsigned.withdrawalId(), SignerService.canonicalHash(validUnsigned), validUnsigned.chainId(), validUnsigned.asset(), validUnsigned.tokenContract(), validUnsigned.from(), validUnsigned.to(), validUnsigned.amount(), validUnsigned.amountAtomic(), validUnsigned.nonce(), validUnsigned.gasLimit(), validUnsigned.maxFeePerGas(), validUnsigned.maxPriorityFeePerGas(), validUnsigned.data(), validUnsigned.expiresAt());
        assertTrue(!service.sign(valid).replayed());
    }

    private static SignerRequest request(String withdrawalId, Credentials credentials, BigInteger atomic) {
        SignerRequest unsigned = new SignerRequest(withdrawalId, "", 11155111L, "ETH", null, credentials.getAddress(), "0x2222222222222222222222222222222222222222", atomic, atomic, BigInteger.ZERO, BigInteger.valueOf(21_000), BigInteger.valueOf(2_000_000_000L), BigInteger.valueOf(1_000_000_000L), "0x", Instant.now().plusSeconds(60));
        return new SignerRequest(unsigned.withdrawalId(), SignerService.canonicalHash(unsigned), unsigned.chainId(), unsigned.asset(), unsigned.tokenContract(), unsigned.from(), unsigned.to(), unsigned.amount(), unsigned.amountAtomic(), unsigned.nonce(), unsigned.gasLimit(), unsigned.maxFeePerGas(), unsigned.maxPriorityFeePerGas(), unsigned.data(), unsigned.expiresAt());
    }

    private static JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:signer-policy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
    }
}
