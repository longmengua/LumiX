package com.lumix.marketdata.contract.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.marketdata.contract.BookDeltaPayload;
import com.lumix.marketdata.contract.BookSnapshotPayload;
import com.lumix.marketdata.contract.MarketDataContractViolation;
import com.lumix.marketdata.contract.MarketDataEventFixtureAccess;
import com.lumix.marketdata.contract.MarketDataPayload;
import com.lumix.marketdata.contract.MarketDataRejectionReason;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.TickerPayload;
import com.lumix.marketdata.contract.TradePayload;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 wire mapping 保留 event identity、時間、精度與每種 sealed payload，且不容忍未知 schema/type。
 */
class NormalizedMarketDataEventWireMapperTest {

    /**
     * 所有 P21-T02 payload variant 都必須在 domain-to-wire-to-domain 後完全保留，作為未來 transport 的固定邊界。
     */
    @Test
    void roundTripPreservesEveryNormalizedPayloadVariant() {
        List<MarketDataPayload> payloads = List.of(
                MarketDataEventFixtureAccess.bookSnapshot(),
                MarketDataEventFixtureAccess.bookDelta(),
                MarketDataEventFixtureAccess.trade(),
                MarketDataEventFixtureAccess.ticker()
        );

        for (MarketDataPayload payload : payloads) {
            NormalizedMarketDataEvent original = MarketDataEventFixtureAccess.event(payload);
            NormalizedMarketDataEventWire wire = NormalizedMarketDataEventWireMapper.toWire(original);
            NormalizedMarketDataEvent restored = NormalizedMarketDataEventWireMapper.fromWire(wire);

            assertEquals(original, restored);
            assertEquals(original.identity(), restored.identity());
        }
    }

    /**
     * serialization 中的 decimal 與 atomic quantity 必須是字串，禁止交給 locale 或數值 formatter 改寫。
     */
    @Test
    void wireUsesPlainStringForDecimalAndAtomicQuantity() {
        NormalizedMarketDataEventWire wire = NormalizedMarketDataEventWireMapper.toWire(
                MarketDataEventFixtureAccess.event(MarketDataEventFixtureAccess.trade())
        );
        MarketDataPayloadWire.Trade payload = (MarketDataPayloadWire.Trade) wire.payload();

        assertEquals("100.25", payload.price());
        assertEquals("125000000", payload.quantityAtoms());
        assertEquals("42", wire.sequence());
    }

    /**
     * 不認得的 schema 或 event type 不得採取最佳努力相容，否則 replay 會失去可審計性。
     */
    @Test
    void rejectsUnknownSchemaVersionAndEventType() {
        NormalizedMarketDataEventWire valid = NormalizedMarketDataEventWireMapper.toWire(
                MarketDataEventFixtureAccess.event(MarketDataEventFixtureAccess.ticker())
        );
        NormalizedMarketDataEventWire unknownSchema = new NormalizedMarketDataEventWire(
                valid.source(), valid.channel(), valid.instrumentId(), valid.eventType(), valid.sequence(),
                valid.sourceTimestamp(), valid.receivedTimestamp(), "2", valid.priceScale(), valid.quantityScale(),
                valid.maximumSignificantDigits(), valid.payload()
        );
        NormalizedMarketDataEventWire unknownType = new NormalizedMarketDataEventWire(
                valid.source(), valid.channel(), valid.instrumentId(), "PROVIDER_PRIVATE_TICKER", valid.sequence(),
                valid.sourceTimestamp(), valid.receivedTimestamp(), valid.schemaVersion(), valid.priceScale(),
                valid.quantityScale(), valid.maximumSignificantDigits(), valid.payload()
        );

        assertReason(MarketDataRejectionReason.UNKNOWN_SCHEMA_VERSION, () -> NormalizedMarketDataEventWireMapper.fromWire(unknownSchema));
        assertReason(MarketDataRejectionReason.UNKNOWN_EVENT_TYPE, () -> NormalizedMarketDataEventWireMapper.fromWire(unknownType));
    }

    private static void assertReason(MarketDataRejectionReason expected, org.junit.jupiter.api.function.Executable executable) {
        assertEquals(expected, assertThrows(MarketDataContractViolation.class, executable).reason());
    }
}
