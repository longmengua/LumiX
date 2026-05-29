/*
 * 檔案用途：測試 Polymarket approval query cache 與 expiry 行為。
 */
package com.example.exchange.domain.service;

import com.example.exchange.infra.config.PolymarketConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.BatchRequest;
import org.web3j.protocol.core.BatchResponse;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.websocket.events.Notification;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import io.reactivex.Flowable;

import static org.assertj.core.api.Assertions.assertThat;

class PolymarketApprovalServiceTest {

    private static final String OWNER =
            "0x0000000000000000000000000000000000000001";

    @Test
    @DisplayName("ERC20 allowance 查詢在 TTL 內會使用 cache，clear owner 後重新 eth_call")
    /**
     * 流程：同一 owner/token/spender 連續查 allowance，然後清除該 owner cache。
     * 期望：第二次查詢不打 RPC；clear 後下一次查詢會重新 eth_call，避免 stale approval 長期存在。
     */
    void collateralAllowanceUsesCacheUntilOwnerClear() {
        CountingWeb3jService web3jService = new CountingWeb3jService(
                uint256("25"),
                uint256("30")
        );
        PolymarketApprovalService service = new PolymarketApprovalService(
                Web3j.build(web3jService),
                configs(30)
        );

        assertThat(service.getCollateralAllowance(OWNER)).isEqualTo(new BigInteger("25"));
        assertThat(service.getCollateralAllowance(OWNER)).isEqualTo(new BigInteger("25"));
        assertThat(web3jService.ethCallCount).isEqualTo(1);

        service.clearApprovalCache(OWNER);

        assertThat(service.getCollateralAllowance(OWNER)).isEqualTo(new BigInteger("30"));
        assertThat(web3jService.ethCallCount).isEqualTo(2);
    }

    @Test
    @DisplayName("ERC1155 approval cache 過期後會重新 eth_call")
    /**
     * 流程：approval cache TTL 設為 1 秒，第一次查到 true，TTL 後 RPC 回 false。
     * 期望：過期後不沿用舊 approval，下一次查詢會重新讀鏈上狀態。
     */
    void conditionalTokensApprovalRefreshesAfterTtl() throws Exception {
        CountingWeb3jService web3jService = new CountingWeb3jService(
                bool(true),
                bool(false)
        );
        PolymarketApprovalService service = new PolymarketApprovalService(
                Web3j.build(web3jService),
                configs(1)
        );

        assertThat(service.isConditionalTokensApproved(OWNER)).isTrue();
        Thread.sleep(1100);

        assertThat(service.isConditionalTokensApproved(OWNER)).isFalse();
        assertThat(web3jService.ethCallCount).isEqualTo(2);
    }

    private static PolymarketConfigs configs(long ttlSeconds) {
        PolymarketConfigs configs = new PolymarketConfigs();
        configs.setApprovalCacheTtlSeconds(ttlSeconds);
        configs.getChain().setCollateralToken("0x0000000000000000000000000000000000000010");
        configs.getChain().setConditionalTokens("0x0000000000000000000000000000000000000020");
        configs.getChain().setNegRiskExchangeV2("0x0000000000000000000000000000000000000030");
        return configs;
    }

    private static String uint256(String value) {
        return "0x" + leftPad(new BigInteger(value).toString(16));
    }

    private static String bool(boolean value) {
        return "0x" + leftPad(value ? "1" : "0");
    }

    private static String leftPad(String hex) {
        return "0".repeat(64 - hex.length()) + hex;
    }

    private static class CountingWeb3jService implements Web3jService {
        private final Queue<String> results = new ArrayDeque<>();
        private int ethCallCount;

        private CountingWeb3jService(String... results) {
            this.results.addAll(java.util.List.of(results));
        }

        @Override
        public <T extends Response> T send(Request request, Class<T> responseType) throws IOException {
            if (!EthCall.class.equals(responseType)) {
                throw new IOException("unsupported response type: " + responseType);
            }

            ethCallCount++;
            EthCall response = new EthCall();
            response.setResult(results.remove());
            return responseType.cast(response);
        }

        @Override
        public <T extends Response> CompletableFuture<T> sendAsync(Request request, Class<T> responseType) {
            CompletableFuture<T> future = new CompletableFuture<>();
            try {
                future.complete(send(request, responseType));
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        @Override
        public BatchResponse sendBatch(BatchRequest batchRequest) {
            throw new UnsupportedOperationException("batch not used");
        }

        @Override
        public CompletableFuture<BatchResponse> sendBatchAsync(BatchRequest batchRequest) {
            throw new UnsupportedOperationException("batch not used");
        }

        @Override
        public <T extends Notification<?>> Flowable<T> subscribe(
                Request request,
                String unsubscribeMethod,
                Class<T> responseType
        ) {
            throw new UnsupportedOperationException("subscribe not used");
        }

        @Override
        public void close() {
        }
    }
}
