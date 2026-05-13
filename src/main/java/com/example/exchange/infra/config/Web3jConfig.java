package com.example.exchange.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Web3j configuration。
 *
 * 目前：
 * - Polygon Mainnet
 * - Polymarket 使用 Polygon chainId=137
 *
 * TODO:
 * 正式環境建議：
 * 1. Alchemy
 * 2. Infura
 * 3. QuickNode
 * 4. 自建 Polygon RPC
 *
 * 不建議長期直接使用 public RPC。
 */
@Slf4j
@Configuration
public class Web3jConfig {

    /**
     * Polygon public RPC.
     *
     * TODO:
     * 正式環境請改自己的 RPC provider。
     */
    private static final String POLYGON_RPC =
            "https://polygon-rpc.com";

    /**
     * Spring Web3j bean。
     */
    @Bean
    public Web3j web3j() {

        log.info(
                "Initializing Web3j Polygon RPC={}",
                POLYGON_RPC
        );

        return Web3j.build(
                new HttpService(POLYGON_RPC)
        );
    }
}
