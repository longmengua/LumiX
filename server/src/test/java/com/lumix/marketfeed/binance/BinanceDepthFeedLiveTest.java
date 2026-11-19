package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 僅人工啟用的真實唯讀網路驗證；一般回歸不可依賴外部供應商。 */
@EnabledIfEnvironmentVariable(named = "LUMIX_TEST_BINANCE_LIVE", matches = "true")
class BinanceDepthFeedLiveTest {
    @Test
    void bothSymbolsReachHealthyAfterSnapshotAndDelta() throws Exception {
        // 真實來源必須先完成快照及增量銜接，才能對外宣稱兩個交易對均健康。
        BinanceDepthFeed feed = new BinanceDepthFeed(new ObjectMapper());
        BinanceKlineFeed klines = new BinanceKlineFeed(new ObjectMapper());
        try {
            Instant deadline = Instant.now().plus(Duration.ofSeconds(25));
            while (Instant.now().isBefore(deadline)) {
                if (feed.view("BTCUSDT").healthy() && feed.view("ETHUSDT").healthy()
                        && klines.view("BTCUSDT", BinanceKlineInterval.MINUTE_1).healthy() && klines.view("ETHUSDT", BinanceKlineInterval.MINUTE_1).healthy()) return;
                Thread.sleep(250);
            }
            assertTrue(feed.view("BTCUSDT").healthy(), "BTCUSDT feed did not become healthy");
            assertTrue(feed.view("ETHUSDT").healthy(), "ETHUSDT feed did not become healthy");
            assertTrue(klines.view("BTCUSDT", BinanceKlineInterval.MINUTE_1).healthy(), "BTCUSDT klines did not become healthy");
            assertTrue(klines.view("ETHUSDT", BinanceKlineInterval.MINUTE_1).healthy(), "ETHUSDT klines did not become healthy");
        } finally {
            feed.close();
            klines.close();
        }
    }
}
