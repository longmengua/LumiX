package com.lumix.marketdata.contract.serialization;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.BookDeltaPayload;
import com.lumix.marketdata.contract.BookLevel;
import com.lumix.marketdata.contract.BookSnapshotPayload;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.InstrumentId;
import com.lumix.marketdata.contract.InstrumentPrecision;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataContractViolation;
import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.MarketDataPayload;
import com.lumix.marketdata.contract.MarketDataRejectionReason;
import com.lumix.marketdata.contract.MarketDataSource;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.SchemaVersion;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.TickerPayload;
import com.lumix.marketdata.contract.TradePayload;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Domain event 與 stable wire record 的雙向 mapping。
 *
 * <p>這不是 transport codec：不產生 JSON、不接網路，也不容忍未知 schema/type。未來 transport 必須先解碼成
 * 此 wire form，再以同一套 fail-closed 規則轉成 domain event。</p>
 */
public final class NormalizedMarketDataEventWireMapper {

    private NormalizedMarketDataEventWireMapper() {
    }

    public static NormalizedMarketDataEventWire toWire(NormalizedMarketDataEvent event) {
        if (event == null) {
            throw new MarketDataContractViolation(MarketDataRejectionReason.NULL_VALUE, "event must not be null");
        }
        InstrumentPrecision precision = event.precision();
        return new NormalizedMarketDataEventWire(
                event.source().value(),
                event.channel().name(),
                event.instrumentId().value(),
                event.eventType().name(),
                event.sequence().toWireString(),
                event.sourceTimestamp().toString(),
                event.receivedTimestamp().toString(),
                Integer.toString(event.schemaVersion().value()),
                precision.priceScale(),
                precision.quantityScale(),
                precision.maximumSignificantDigits(),
                toWirePayload(event.payload())
        );
    }

    public static NormalizedMarketDataEvent fromWire(NormalizedMarketDataEventWire wire) {
        if (wire == null) {
            throw new MarketDataContractViolation(MarketDataRejectionReason.NULL_VALUE, "wire event must not be null");
        }
        InstrumentPrecision precision = new InstrumentPrecision(
                wire.priceScale(),
                wire.quantityScale(),
                wire.maximumSignificantDigits()
        );
        MarketDataEventType eventType = MarketDataEventType.fromWire(wire.eventType());
        MarketDataPayload payload = fromWirePayload(wire.payload(), precision);
        return new NormalizedMarketDataEvent(
                new MarketDataSource(wire.source()),
                MarketDataChannel.fromWire(wire.channel()),
                new InstrumentId(wire.instrumentId()),
                eventType,
                Sequence.fromWire(wire.sequence()),
                parseTimestamp(wire.sourceTimestamp(), "sourceTimestamp"),
                parseTimestamp(wire.receivedTimestamp(), "receivedTimestamp"),
                SchemaVersion.fromWire(wire.schemaVersion()),
                precision,
                payload
        );
    }

    private static MarketDataPayloadWire toWirePayload(MarketDataPayload payload) {
        if (payload instanceof BookSnapshotPayload snapshot) {
            return new MarketDataPayloadWire.BookSnapshot(toWireLevels(snapshot.bids()), toWireLevels(snapshot.asks()));
        }
        if (payload instanceof BookDeltaPayload delta) {
            return new MarketDataPayloadWire.BookDelta(toWireLevels(delta.bidUpdates()), toWireLevels(delta.askUpdates()));
        }
        if (payload instanceof TradePayload trade) {
            return new MarketDataPayloadWire.Trade(
                    trade.tradeId(),
                    trade.price().toWireString(),
                    trade.quantity().toWireString()
            );
        }
        if (payload instanceof TickerPayload ticker) {
            return new MarketDataPayloadWire.Ticker(
                    ticker.lastPrice().toWireString(),
                    ticker.baseVolume().toWireString()
            );
        }
        // sealed payload 理論上不會到達此處；保守拒絕可防止未來放寬 sealed boundary 時靜默序列化 provider payload。
        throw new MarketDataContractViolation(
                MarketDataRejectionReason.PROVIDER_SPECIFIC_PAYLOAD,
                "payload is not a normalized market-data payload"
        );
    }

    private static MarketDataPayload fromWirePayload(MarketDataPayloadWire payload, InstrumentPrecision precision) {
        if (payload instanceof MarketDataPayloadWire.BookSnapshot snapshot) {
            return new BookSnapshotPayload(fromWireLevels(snapshot.bids(), precision), fromWireLevels(snapshot.asks(), precision));
        }
        if (payload instanceof MarketDataPayloadWire.BookDelta delta) {
            return new BookDeltaPayload(
                    fromWireLevels(delta.bidUpdates(), precision),
                    fromWireLevels(delta.askUpdates(), precision)
            );
        }
        if (payload instanceof MarketDataPayloadWire.Trade trade) {
            return new TradePayload(
                    trade.tradeId(),
                    DecimalPrice.fromWire(trade.price(), precision),
                    AtomicQuantity.fromWire(trade.quantityAtoms(), precision)
            );
        }
        if (payload instanceof MarketDataPayloadWire.Ticker ticker) {
            return new TickerPayload(
                    DecimalPrice.fromWire(ticker.lastPrice(), precision),
                    AtomicQuantity.fromWire(ticker.baseVolumeAtoms(), precision)
            );
        }
        throw new MarketDataContractViolation(
                MarketDataRejectionReason.PROVIDER_SPECIFIC_PAYLOAD,
                "wire payload is missing or provider-specific"
        );
    }

    private static List<MarketDataPayloadWire.BookLevel> toWireLevels(List<BookLevel> levels) {
        return levels.stream()
                .map(level -> new MarketDataPayloadWire.BookLevel(
                        level.price().toWireString(),
                        level.quantity().toWireString()
                ))
                .toList();
    }

    private static List<BookLevel> fromWireLevels(
            List<MarketDataPayloadWire.BookLevel> levels,
            InstrumentPrecision precision
    ) {
        if (levels == null) {
            throw new MarketDataContractViolation(MarketDataRejectionReason.NULL_VALUE, "wire book levels must not be null");
        }
        try {
            return levels.stream()
                    .map(level -> new BookLevel(
                            DecimalPrice.fromWire(level.price(), precision),
                            AtomicQuantity.fromWire(level.quantityAtoms(), precision)
                    ))
                    .toList();
        } catch (NullPointerException exception) {
            throw new MarketDataContractViolation(MarketDataRejectionReason.NULL_VALUE, "wire book level must not be null");
        }
    }

    private static Instant parseTimestamp(String value, String fieldName) {
        if (value == null) {
            throw new MarketDataContractViolation(MarketDataRejectionReason.MISSING_TIMESTAMP, fieldName + " must not be null");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new MarketDataContractViolation(MarketDataRejectionReason.MISSING_TIMESTAMP, fieldName + " must be ISO-8601 instant");
        }
    }
}
