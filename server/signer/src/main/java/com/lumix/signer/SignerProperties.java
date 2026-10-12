package com.lumix.signer;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** signer 自己持有的最小安全設定。password 只由部署 secret 注入，禁止記錄或回傳。 */
@ConfigurationProperties("lumix.signer")
public class SignerProperties {
    private long chainId = 11155111L;
    private String network = "SEPOLIA";
    private String sourceAddress;
    private Set<String> allowedAssets = Set.of("ETH", "USDT");
    private String sepoliaUsdtContract;
    private BigDecimal singleSigningLimit = BigDecimal.ZERO;
    private BigDecimal dailySigningLimit = BigDecimal.ZERO;
    private Duration requestTtl = Duration.ofMinutes(5);
    private Path keystorePath;
    private String keystorePassword;
    public long getChainId(){return chainId;} public void setChainId(long v){chainId=v;}
    public String getNetwork(){return network;} public void setNetwork(String v){network=v;}
    public String getSourceAddress(){return sourceAddress;} public void setSourceAddress(String v){sourceAddress=v;}
    public Set<String> getAllowedAssets(){return allowedAssets;} public void setAllowedAssets(Set<String> v){allowedAssets=v;}
    public String getSepoliaUsdtContract(){return sepoliaUsdtContract;} public void setSepoliaUsdtContract(String v){sepoliaUsdtContract=v;}
    public BigDecimal getSingleSigningLimit(){return singleSigningLimit;} public void setSingleSigningLimit(BigDecimal v){singleSigningLimit=v;}
    public BigDecimal getDailySigningLimit(){return dailySigningLimit;} public void setDailySigningLimit(BigDecimal v){dailySigningLimit=v;}
    public Duration getRequestTtl(){return requestTtl;} public void setRequestTtl(Duration v){requestTtl=v;}
    public Path getKeystorePath(){return keystorePath;} public void setKeystorePath(Path v){keystorePath=v;}
    public String getKeystorePassword(){return keystorePassword;} public void setKeystorePassword(String v){keystorePassword=v;}
}
