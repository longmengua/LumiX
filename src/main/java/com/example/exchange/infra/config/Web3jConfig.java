/*
 * 檔案用途：基礎設施設定，建立 Spring Bean 並連接 Kafka、Redis、Web3j 或 HTTP client。
 */
package com.example.exchange.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Web3j configuration。
 */
@Slf4j
@Configuration
public class Web3jConfig {

    @Value("${web3.polygon-rpc-url:https://polygon-bor-rpc.publicnode.com}")
    private String polygonRpcUrl;

    @Bean
    public Web3j web3j() {
        log.info("Initializing Web3j Polygon RPC={}", polygonRpcUrl);

        return Web3j.build(
                new HttpService(polygonRpcUrl)
        );
    }
}