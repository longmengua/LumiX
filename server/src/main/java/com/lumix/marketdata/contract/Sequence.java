package com.lumix.marketdata.contract;

/**
 * 同一 {@link StreamKey} 內的正整數來源 sequence。
 */
public record Sequence(long value) {

    public Sequence {
        if (value <= 0) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.NON_POSITIVE_SEQUENCE,
                    "sequence must be positive"
            );
        }
    }

    public static Sequence fromWire(String value) {
        try {
            return new Sequence(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.NON_POSITIVE_SEQUENCE,
                    "sequence must be a positive long"
            );
        }
    }

    public String toWireString() {
        return Long.toString(value);
    }
}
