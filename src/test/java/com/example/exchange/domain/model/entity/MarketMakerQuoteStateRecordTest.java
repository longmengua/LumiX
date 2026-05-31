/*
 * 檔案用途：測試 market-maker quote state JPA record 與 DTO 之間的重啟/重載 round-trip。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerQuoteStateRecordTest {

    @Test
    @DisplayName("MarketMakerQuoteStateRecord round-trip 會保留 active quote ownership 與 per-side version metadata")
    void quoteStateRecordRoundTripPreservesVersionMetadata() {
        UUID bidOrderId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID askOrderId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID replacedBidOrderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID replacedAskOrderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        MarketMakerQuoteState state = new MarketMakerQuoteState(
                "mm-1",
                9101,
                "BTCUSDT",
                "quote-ref-2",
                true,
                true,
                "ACCEPTED",
                2,
                bidOrderId,
                askOrderId,
                3,
                4,
                replacedBidOrderId,
                replacedAskOrderId,
                Instant.parse("2026-06-01T00:00:00Z")
        );

        // 場景：JPA 重新載入 active quote state 後，operator reconciliation 仍能看到目前與上一輪 bid/ask ownership。
        MarketMakerQuoteState restored = MarketMakerQuoteStateRecord.from(state).toState();

        assertThat(restored).isEqualTo(state);
    }
}
