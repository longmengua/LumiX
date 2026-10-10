package com.lumix.ethereum.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ethereum Mainnet 唯讀觀測設定。
 *
 * <p>RPC URL 只從部署 secret 注入；此型別刻意沒有 getter log 或 toString，避免 endpoint 內的 API key
 * 出現在診斷輸出。開關預設關閉，沒有完整 provider 設定時不得嘗試連線。</p>
 */
@ConfigurationProperties("lumix.ethereum")
public class EthereumProperties {
    private boolean observationEnabled = false;
    private String rpcUrl;
    private long chainId = 1L;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(15);

    public boolean isObservationEnabled() { return observationEnabled; }
    public void setObservationEnabled(boolean observationEnabled) { this.observationEnabled = observationEnabled; }
    public String getRpcUrl() { return rpcUrl; }
    public void setRpcUrl(String rpcUrl) { this.rpcUrl = rpcUrl; }
    public long getChainId() { return chainId; }
    public void setChainId(long chainId) { this.chainId = chainId; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    /** 在建立 network client 前集中驗證，避免部分 scanner 帶著不完整設定執行。 */
    public void validateForObservation() {
        if (!observationEnabled) throw new IllegalStateException("Ethereum observation is disabled");
        if (chainId != 1L) throw new IllegalStateException("only Ethereum Mainnet chainId 1 is supported");
        if (rpcUrl == null || rpcUrl.isBlank()) throw new IllegalStateException("LUMIX_ETHEREUM_RPC_URL is required when observation is enabled");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(readTimeout, "readTimeout must not be null");
        if (connectTimeout.isNegative() || connectTimeout.isZero() || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalStateException("Ethereum RPC timeouts must be positive");
        }
    }
}
