package com.lumix.withdrawal.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 提款風控與地址等待期設定；金額使用 BigDecimal 避免 binary floating point。 */
@ConfigurationProperties("lumix.withdrawal.platform")
public class WithdrawalPlatformProperties {
    private Duration addressCooldown = Duration.ofHours(24);
    private Duration passwordChangeCooldown = Duration.ofHours(24);
    private Duration maximumSessionAge = Duration.ofHours(8);
    private BigDecimal manualReviewAmount = new BigDecimal("1000");
    private BigDecimal dualReviewAmount = new BigDecimal("10000");
    private String sepoliaSourceAddress;
    private String signerUrl;
    private Duration signingIntentTtl = Duration.ofMinutes(5);
    private String sepoliaUsdtContract;
    private BigInteger ethGasLimit = BigInteger.valueOf(21_000L);
    private BigInteger erc20GasLimit = BigInteger.valueOf(65_000L);
    private BigInteger maxFeePerGas = BigInteger.valueOf(2_000_000_000L);
    private BigInteger maxPriorityFeePerGas = BigInteger.valueOf(1_000_000_000L);
    private int tenMinuteCount = 3;
    private int oneHourCount = 10;
    private int dailyCount = 20;
    private BigDecimal dailyAmount = new BigDecimal("10000");
    public Duration getAddressCooldown() { return addressCooldown; }
    public void setAddressCooldown(Duration value) { addressCooldown = value; }
    public Duration getPasswordChangeCooldown() { return passwordChangeCooldown; }
    public void setPasswordChangeCooldown(Duration value) { passwordChangeCooldown = value; }
    /** 尚未有 step-up credential 時，提款只接受受限時效內的已認證 session。 */
    public Duration getMaximumSessionAge() { return maximumSessionAge; }
    public void setMaximumSessionAge(Duration value) { maximumSessionAge = value; }
    public BigDecimal getManualReviewAmount() { return manualReviewAmount; }
    public void setManualReviewAmount(BigDecimal value) { manualReviewAmount = value; }
    /** 高額提款須兩名不同 reviewer 留下相同 immutable payload hash 的核准證據。 */
    public BigDecimal getDualReviewAmount() { return dualReviewAmount; }
    public void setDualReviewAmount(BigDecimal value) { dualReviewAmount = value; }
    public String getSepoliaSourceAddress() { return sepoliaSourceAddress; }
    public void setSepoliaSourceAddress(String value) { sepoliaSourceAddress = value; }
    public String getSignerUrl() { return signerUrl; }
    public void setSignerUrl(String value) { signerUrl = value; }
    public Duration getSigningIntentTtl() { return signingIntentTtl; }
    public void setSigningIntentTtl(Duration value) { signingIntentTtl = value; }
    public String getSepoliaUsdtContract() { return sepoliaUsdtContract; }
    public void setSepoliaUsdtContract(String value) { sepoliaUsdtContract = value; }
    public BigInteger getEthGasLimit() { return ethGasLimit; }
    public void setEthGasLimit(BigInteger value) { ethGasLimit = value; }
    public BigInteger getErc20GasLimit() { return erc20GasLimit; }
    public void setErc20GasLimit(BigInteger value) { erc20GasLimit = value; }
    public BigInteger getMaxFeePerGas() { return maxFeePerGas; }
    public void setMaxFeePerGas(BigInteger value) { maxFeePerGas = value; }
    public BigInteger getMaxPriorityFeePerGas() { return maxPriorityFeePerGas; }
    public void setMaxPriorityFeePerGas(BigInteger value) { maxPriorityFeePerGas = value; }
    public int getTenMinuteCount() { return tenMinuteCount; }
    public void setTenMinuteCount(int value) { tenMinuteCount = value; }
    public int getOneHourCount() { return oneHourCount; }
    public void setOneHourCount(int value) { oneHourCount = value; }
    public int getDailyCount() { return dailyCount; }
    public void setDailyCount(int value) { dailyCount = value; }
    public BigDecimal getDailyAmount() { return dailyAmount; }
    public void setDailyAmount(BigDecimal value) { dailyAmount = value; }
}
