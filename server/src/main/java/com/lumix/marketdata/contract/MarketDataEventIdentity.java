package com.lumix.marketdata.contract;

/**
 * T02 定義的 duplicate identity key。
 *
 * <p>identity 故意不含 received timestamp 或任何本機 clock 值。duplicate 接受、拒絕或隔離決策由 P21-T03 負責。</p>
 */
public record MarketDataEventIdentity(
        MarketDataSource source,
        MarketDataChannel channel,
        InstrumentId instrumentId,
        MarketDataEventType eventType,
        Sequence sequence,
        String payloadFingerprint
) {

    public MarketDataEventIdentity {
        MarketDataContractValidation.requireValue(source, "source");
        MarketDataContractValidation.requireValue(channel, "channel");
        MarketDataContractValidation.requireValue(instrumentId, "instrumentId");
        MarketDataContractValidation.requireValue(eventType, "eventType");
        MarketDataContractValidation.requireValue(sequence, "sequence");
        payloadFingerprint = MarketDataContractValidation.requireText(
                payloadFingerprint,
                "payloadFingerprint",
                java.util.regex.Pattern.compile("[0-9a-f]{64}")
        );
    }
}
