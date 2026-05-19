/*
 * 檔案用途：Spring Boot 應用程式入口，負責啟動交易所核心與 Polymarket 整合服務。
 */
package com.example.exchange;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.infra.config.FundingRateProperties;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.example.exchange.infra.config.ReconciliationProperties;
import com.example.exchange.infra.config.RiskControlsProperties;
import com.example.exchange.infra.config.RiskSnapshotProperties;
import com.example.exchange.infra.config.SecurityControlsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling
@EnableJpaRepositories(
        basePackages = "com.example.exchange.domain.repository.jpa"
)
@EntityScan(
        basePackages = "com.example.exchange.domain.model.entity"
)
@EnableConfigurationProperties({
        ApiAuthProperties.class,
        FundingRateProperties.class,
        MarkPriceOracleProperties.class,
        PolymarketConfigs.class,
        ReconciliationProperties.class,
        RiskControlsProperties.class,
        RiskSnapshotProperties.class,
        SecurityControlsProperties.class
})
public class ExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeApplication.class, args);
    }
}
