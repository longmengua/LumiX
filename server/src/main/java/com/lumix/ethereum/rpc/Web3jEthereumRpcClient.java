package com.lumix.ethereum.rpc;

import com.lumix.ethereum.config.EthereumProperties;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

/**
 * 以 Web3j 存取 Ethereum JSON-RPC 的唯讀 adapter。
 *
 * <p>adapter 不識別 Alchemy 特有 API，也不建立 Credentials；provider URL 只傳給 HTTP transport，絕不寫入
 * log 或 exception 訊息。網路錯誤以 IOException 上拋，scanner 必須不前移 checkpoint。</p>
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.ethereum", name = "observation-enabled", havingValue = "true")
public final class Web3jEthereumRpcClient implements EthereumRpcClient, AutoCloseable {
    private final Web3j web3j;

    public Web3jEthereumRpcClient(EthereumProperties properties) {
        EthereumProperties configured = Objects.requireNonNull(properties, "properties must not be null");
        configured.validateForObservation();
        OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(configured.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(configured.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build();
        this.web3j = Web3j.build(new HttpService(configured.getRpcUrl(), httpClient, false));
    }

    @Override
    public EthereumBlock finalizedBlock() throws IOException {
        return toDomain(web3j.ethGetBlockByNumber(DefaultBlockParameterName.FINALIZED, false).send());
    }

    @Override
    public EthereumBlock blockByNumber(BigInteger number) throws IOException {
        return toDomain(web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number), false).send());
    }

    private static EthereumBlock toDomain(EthBlock response) throws IOException {
        if (response.hasError() || response.getBlock() == null) {
            // provider error 細節可能含 endpoint 或 request metadata，因此不把它帶入上層例外／日誌。
            throw new IOException("Ethereum RPC block lookup failed");
        }
        EthBlock.Block block = response.getBlock();
        return new EthereumBlock(block.getNumber(), block.getHash(), Instant.ofEpochSecond(block.getTimestamp().longValueExact()));
    }

    @Override
    public void close() {
        web3j.shutdown();
    }
}
