/*
 * 檔案用途：Spring Boot 應用程式入口，負責啟動交易所核心與 Polymarket 整合服務。
 */
package com.example.exchange;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.infra.config.ArchiveExporterProperties;
import com.example.exchange.infra.config.AlertBackendProperties;
import com.example.exchange.infra.config.BonusCreditProperties;
import com.example.exchange.infra.config.FundingRateProperties;
import com.example.exchange.infra.config.HedgeVenueCallbackProperties;
import com.example.exchange.infra.config.LedgerArchiveProperties;
import com.example.exchange.infra.config.MatchingWorkerProperties;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import com.example.exchange.infra.config.MarketDataRetentionProperties;
import com.example.exchange.infra.config.MarketMakerApiProperties;
import com.example.exchange.infra.config.MarketMakerAutoQuoteProperties;
import com.example.exchange.infra.config.MatchingBookRecoveryProperties;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.example.exchange.infra.config.PushGatewayProperties;
import com.example.exchange.infra.config.ReconciliationProperties;
import com.example.exchange.infra.config.RiskControlsProperties;
import com.example.exchange.infra.config.RiskSnapshotProperties;
import com.example.exchange.infra.config.SecurityControlsProperties;
import com.example.exchange.infra.config.TracingExportProperties;
import com.example.exchange.infra.config.TurnoverReconciliationProperties;
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
        ArchiveExporterProperties.class,
        AlertBackendProperties.class,
        BonusCreditProperties.class,
        FundingRateProperties.class,
        HedgeVenueCallbackProperties.class,
        LedgerArchiveProperties.class,
        MatchingWorkerProperties.class,
        MarkPriceOracleProperties.class,
        MarketDataRetentionProperties.class,
        MarketMakerApiProperties.class,
        MarketMakerAutoQuoteProperties.class,
        MatchingBookRecoveryProperties.class,
        PolymarketConfigs.class,
        PushGatewayProperties.class,
        ReconciliationProperties.class,
        RiskControlsProperties.class,
        RiskSnapshotProperties.class,
        SecurityControlsProperties.class,
        TracingExportProperties.class,
        TurnoverReconciliationProperties.class
})
public class ExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeApplication.class, args);
    }
}
