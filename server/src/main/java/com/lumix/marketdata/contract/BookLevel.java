package com.lumix.marketdata.contract;

/**
 * Snapshot 或 delta 使用的單一價位資料。
 *
 * <p>是否允許零量刪除、crossed book 或 snapshot/delta 連續性由後續 T03/T04 定義；本類別只保留精確值。</p>
 */
public record BookLevel(DecimalPrice price, AtomicQuantity quantity) {

    public BookLevel {
        MarketDataContractValidation.requireValue(price, "price");
        MarketDataContractValidation.requireValue(quantity, "quantity");
        if (!price.isPositive()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_DECIMAL,
                    "book level price must be positive"
            );
        }
    }
}
