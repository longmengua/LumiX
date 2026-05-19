/*
 * 檔案用途：測試 mark/index price oracle 的啟動載入、更新與 stale 判斷。
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.MarkPriceOracleProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkPriceOracleServiceTest {

    @Test
    @DisplayName("啟動設定會載入 mark/index price 並正規化 symbol")
    /**
     * 流程：準備設定檔價格 -> 載入 oracle -> 驗證查詢 symbol 不分大小寫且價格新鮮。
     */
    void loadConfiguredPricesNormalizesSymbol() {
        MarkPriceOracleService service = new MarkPriceOracleService(properties("btcusdt", "101", "100", 30_000));

        service.loadConfiguredPrices();

        var snapshot = service.snapshot("BTCUSDT").orElseThrow();
        assertThat(snapshot.symbol()).isEqualTo("BTCUSDT");
        assertThat(snapshot.markPrice()).isEqualByComparingTo("101");
        assertThat(snapshot.indexPrice()).isEqualByComparingTo("100");
        assertThat(snapshot.source()).isEqualTo("config");
        assertThat(snapshot.stale()).isFalse();
    }

    @Test
    @DisplayName("手動更新會覆蓋 mark/index price 並拒絕非正價格")
    /**
     * 流程：更新同一 symbol 的 oracle quote -> 驗證新價格覆蓋，且非正 mark/index price 會被拒絕。
     */
    void updateReplacesQuoteAndRejectsNonPositivePrice() {
        MarkPriceOracleService service = new MarkPriceOracleService(new MarkPriceOracleProperties());

        service.update("BTCUSDT", new BigDecimal("105"), new BigDecimal("104"), "manual");

        var snapshot = service.requireFresh("btcusdt");
        assertThat(snapshot.markPrice()).isEqualByComparingTo("105");
        assertThat(snapshot.indexPrice()).isEqualByComparingTo("104");
        assertThat(snapshot.source()).isEqualTo("manual");
        assertThatThrownBy(() -> service.update("BTCUSDT", BigDecimal.ZERO, new BigDecimal("104"), "manual"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("超過 max staleness 的 quote 會被 requireFresh 拒絕")
    /**
     * 流程：設定極短 stale 門檻 -> 更新 quote -> 等待過期 -> 驗證 requireFresh 會拒絕 stale quote。
     */
    void requireFreshRejectsStaleQuote() throws InterruptedException {
        MarkPriceOracleProperties properties = new MarkPriceOracleProperties();
        properties.setMaxStalenessMs(1);
        MarkPriceOracleService service = new MarkPriceOracleService(properties);

        service.update("BTCUSDT", new BigDecimal("105"), new BigDecimal("104"), "manual");
        Thread.sleep(5);

        assertThat(service.snapshot("BTCUSDT").orElseThrow().stale()).isTrue();
        assertThatThrownBy(() -> service.requireFresh("BTCUSDT"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static MarkPriceOracleProperties properties(
            String symbol,
            String markPrice,
            String indexPrice,
            long maxStalenessMs
    ) {
        MarkPriceOracleProperties.Price price = new MarkPriceOracleProperties.Price();
        price.setSymbol(symbol);
        price.setMarkPrice(new BigDecimal(markPrice));
        price.setIndexPrice(new BigDecimal(indexPrice));
        price.setSource("config");

        MarkPriceOracleProperties properties = new MarkPriceOracleProperties();
        properties.setMaxStalenessMs(maxStalenessMs);
        properties.setPrices(List.of(price));
        return properties;
    }
}
