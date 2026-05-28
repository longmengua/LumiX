/*
 * 檔案用途：將標準化 venue fill message 轉成 durable hedge fill audit record。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.dto.HedgeVenueFillMessage;

public class HedgeVenueFillMapper {

    public HedgeFillRecord toRecord(HedgeVenueFillMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("hedge venue fill message cannot be null");
        }
        return new HedgeFillRecord(
                null,
                normalizeRequired(message.marketMakerId(), "market maker id"),
                normalizeRequired(message.symbol(), "symbol").toUpperCase(),
                normalizeRequired(message.venueOrderId(), "venue order id"),
                normalizeRequired(message.venueFillId(), "venue fill id"),
                message.side(),
                message.quantity(),
                message.price(),
                message.fee(),
                normalizeOptional(message.feeAsset()),
                normalizeOptional(message.refId()),
                message.filledAt(),
                null
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }
}
