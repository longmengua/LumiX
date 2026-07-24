package com.lumix.marketdata.contract;

/**
 * Normalized event 的 schema version。
 *
 * <p>T02 僅認可 version 1。未知 version 必須 fail closed，不能以推測欄位相容性的方式放行資料。</p>
 */
public record SchemaVersion(int value) {

    public static final int CURRENT_VALUE = 1;
    public static final SchemaVersion V1 = new SchemaVersion(CURRENT_VALUE);

    public SchemaVersion {
        if (value != CURRENT_VALUE) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.UNKNOWN_SCHEMA_VERSION,
                    "unsupported schema version: " + value
            );
        }
    }

    public static SchemaVersion fromWire(String value) {
        try {
            return new SchemaVersion(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.UNKNOWN_SCHEMA_VERSION,
                    "schemaVersion must be an integer"
            );
        }
    }
}
