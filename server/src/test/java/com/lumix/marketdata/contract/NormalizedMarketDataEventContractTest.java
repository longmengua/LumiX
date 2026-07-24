package com.lumix.marketdata.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 normalized event envelope 的 identity、時間與 payload 邊界。
 */
class NormalizedMarketDataEventContractTest {

    /**
     * identity 必須包含完整 stream 與 payload fingerprint，卻不能把接收時間當成 identity 的一部分。
     */
    @Test
    void identityUsesFullStreamAndPayloadFingerprintWithoutLocalClock() {
        NormalizedMarketDataEvent first = MarketDataEventFixture.event(MarketDataEventFixture.trade());
        NormalizedMarketDataEvent samePayloadLaterReceived = new NormalizedMarketDataEvent(
                first.source(), first.channel(), first.instrumentId(), first.eventType(), first.sequence(),
                first.sourceTimestamp(), Instant.parse("2026-07-25T00:00:02Z"), first.schemaVersion(),
                first.precision(), first.payload()
        );
        NormalizedMarketDataEvent otherInstrument = new NormalizedMarketDataEvent(
                first.source(), first.channel(), new InstrumentId("ETH-USDT"), first.eventType(), first.sequence(),
                first.sourceTimestamp(), first.receivedTimestamp(), first.schemaVersion(), first.precision(), first.payload()
        );

        assertEquals(first.identity(), samePayloadLaterReceived.identity());
        assertNotEquals(first.identity(), otherInstrument.identity());
        assertEquals(new StreamKey(first.source(), MarketDataChannel.TRADES, first.instrumentId()), first.streamKey());
    }

    /**
     * event type 與 sealed payload 不一致時必須在 contract 邊界拒絕，避免後續 runtime 猜測資料語意。
     */
    @Test
    void rejectsPayloadEventTypeMismatch() {
        MarketDataContractViolation violation = assertThrows(MarketDataContractViolation.class, () ->
                new NormalizedMarketDataEvent(
                        new MarketDataSource("fixture-source"), MarketDataChannel.TICKER, new InstrumentId("BTC-USDT"),
                        MarketDataEventType.TICKER, new Sequence(1), MarketDataEventFixture.SOURCE_TIME,
                        MarketDataEventFixture.RECEIVED_TIME, SchemaVersion.V1, MarketDataEventFixture.PRECISION,
                        MarketDataEventFixture.trade()
                )
        );

        assertEquals(MarketDataRejectionReason.PAYLOAD_EVENT_TYPE_MISMATCH, violation.reason());
    }

    /**
     * source 與 received 時間都是真實輸入欄位，不能用本機 clock 或其中一者補成另一者。
     */
    @Test
    void rejectsMissingSourceOrReceivedTimestamp() {
        MarketDataContractViolation sourceViolation = assertThrows(MarketDataContractViolation.class, () ->
                new NormalizedMarketDataEvent(
                        new MarketDataSource("fixture-source"), MarketDataChannel.TRADES, new InstrumentId("BTC-USDT"),
                        MarketDataEventType.TRADE, new Sequence(1), null, MarketDataEventFixture.RECEIVED_TIME,
                        SchemaVersion.V1, MarketDataEventFixture.PRECISION, MarketDataEventFixture.trade()
                )
        );
        MarketDataContractViolation receivedViolation = assertThrows(MarketDataContractViolation.class, () ->
                new NormalizedMarketDataEvent(
                        new MarketDataSource("fixture-source"), MarketDataChannel.TRADES, new InstrumentId("BTC-USDT"),
                        MarketDataEventType.TRADE, new Sequence(1), MarketDataEventFixture.SOURCE_TIME, null,
                        SchemaVersion.V1, MarketDataEventFixture.PRECISION, MarketDataEventFixture.trade()
                )
        );

        assertEquals(MarketDataRejectionReason.MISSING_TIMESTAMP, sourceViolation.reason());
        assertEquals(MarketDataRejectionReason.MISSING_TIMESTAMP, receivedViolation.reason());
    }

    /**
     * 空白或格式不符的 instrument 不得被 trim、轉大小寫或猜測成合法 identity。
     */
    @Test
    void rejectsBlankAndInvalidInstrumentWithoutNormalization() {
        MarketDataContractViolation blank = assertThrows(MarketDataContractViolation.class, () -> new InstrumentId("  "));
        MarketDataContractViolation lowercase = assertThrows(MarketDataContractViolation.class, () -> new InstrumentId("btc-usdt"));

        assertEquals(MarketDataRejectionReason.BLANK_IDENTIFIER, blank.reason());
        assertEquals(MarketDataRejectionReason.INVALID_IDENTIFIER, lowercase.reason());
    }

    /**
     * sequence 是來源流內正整數；T02 不做跨 stream 排序或 duplicate 決策。
     */
    @Test
    void rejectsNonPositiveSequence() {
        MarketDataContractViolation zero = assertThrows(MarketDataContractViolation.class, () -> new Sequence(0));
        MarketDataContractViolation negative = assertThrows(MarketDataContractViolation.class, () -> new Sequence(-1));

        assertEquals(MarketDataRejectionReason.NON_POSITIVE_SEQUENCE, zero.reason());
        assertEquals(MarketDataRejectionReason.NON_POSITIVE_SEQUENCE, negative.reason());
    }
}
