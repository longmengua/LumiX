package com.lumix.marketdata.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 event payload 可安全跨 thread 傳遞，不會保留呼叫端可變 list 的參考。
 */
class MarketDataPayloadImmutabilityTest {

    /**
     * defensive copy 是 replay contract 的必要條件：事件建立後，外部 list 變動不得改寫已計算的 identity。
     */
    @Test
    void bookPayloadDefensivelyCopiesAndExposesImmutableLists() {
        List<BookLevel> bids = new ArrayList<>(List.of(
                new BookLevel(MarketDataEventFixture.price("100.25"), MarketDataEventFixture.quantity("1"))
        ));
        BookSnapshotPayload payload = new BookSnapshotPayload(bids, List.of());

        bids.clear();

        assertEquals(1, payload.bids().size());
        assertThrows(UnsupportedOperationException.class, () -> payload.bids().add(
                new BookLevel(MarketDataEventFixture.price("100.26"), MarketDataEventFixture.quantity("1"))
        ));
    }
}
