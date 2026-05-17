/*
 * 檔案用途：基礎設施設定，建立 Spring Bean 並連接 Kafka、Redis、Web3j 或 HTTP client。
 */
package com.example.exchange.infra.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp Config。
 *
 * 用途：
 * 1. 提供全域 OkHttpClient Bean
 * 2. 給 Gamma API / 未來 CLOB API 共用
 * 3. 避免每次 request 都 new client
 */
@Configuration
public class OkHttpConfig {

    /**
     * 全域 OkHttpClient Bean。
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()

                /**
                 * 建立 TCP timeout。
                 */
                .connectTimeout(Duration.ofSeconds(10))

                /**
                 * 讀取 timeout。
                 */
                .readTimeout(Duration.ofSeconds(30))

                /**
                 * 寫入 timeout。
                 */
                .writeTimeout(Duration.ofSeconds(30))

                /**
                 * Connection Pool。
                 *
                 * 最多保留 20 條 idle connection。
                 * 5 分鐘回收。
                 */
                .connectionPool(
                        new ConnectionPool(
                                20,
                                5,
                                TimeUnit.MINUTES
                        )
                )

                /**
                 * 發生 connection failure 時自動 retry。
                 */
                .retryOnConnectionFailure(true)

                .build();
    }
}