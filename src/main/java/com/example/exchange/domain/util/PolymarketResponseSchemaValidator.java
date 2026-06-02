/*
 * 檔案用途：領域工具，集中檢查 Polymarket Gamma/CLOB response schema 漂移。
 */
package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PolymarketResponseSchemaReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PolymarketResponseSchemaValidator {

    public static final String GAMMA_EVENTS_V1 = "gamma.events.v1";
    public static final String GAMMA_MARKETS_V1 = "gamma.markets.v1";
    public static final String CLOB_ORDER_OPERATIONS_V1 = "clob.order-operations.v1";

    private static final Set<String> GAMMA_EVENT_FIELDS = Set.of(
            "slug",
            "title",
            "startTime",
            "eventDate",
            "seriesSlug",
            "markets"
    );

    private static final Set<String> GAMMA_MARKET_FIELDS = Set.of(
            "id",
            "conditionId",
            "question",
            "slug",
            "active",
            "closed",
            "acceptingOrders",
            "enableOrderBook",
            "negRisk",
            "endDate",
            "groupItemTitle",
            "groupItemThreshold",
            "sportsMarketType",
            "outcomes",
            "outcomePrices",
            "clobTokenIds",
            "lastTradePrice",
            "bestBid",
            "bestAsk",
            "liquidityNum",
            "volumeNum",
            "volume24hr",
            "events"
    );

    private static final Set<String> CLOB_ORDER_FIELDS = Set.of(
            "success",
            "status",
            "orderID",
            "orderId",
            "id",
            "error",
            "errorMsg",
            "message",
            "httpCode",
            "size_matched",
            "sizeMatched",
            "matched_size",
            "raw"
    );

    private PolymarketResponseSchemaValidator() {
    }

    public static PolymarketResponseSchemaReport validateGammaEventsPage(
            ObjectMapper objectMapper,
            String endpoint,
            String responseBody
    ) {
        return validateArray(
                objectMapper,
                GAMMA_EVENTS_V1,
                "GAMMA",
                endpoint,
                responseBody,
                GAMMA_EVENT_FIELDS,
                Set.of("slug", "title")
        );
    }

    public static PolymarketResponseSchemaReport validateGammaMarketsPage(
            ObjectMapper objectMapper,
            String endpoint,
            String responseBody
    ) {
        return validateArray(
                objectMapper,
                GAMMA_MARKETS_V1,
                "GAMMA",
                endpoint,
                responseBody,
                GAMMA_MARKET_FIELDS,
                Set.of("slug")
        );
    }

    public static PolymarketResponseSchemaReport validateClobOrderOperation(
            ObjectMapper objectMapper,
            String operation,
            String responseBody
    ) {
        return validateObject(
                objectMapper,
                CLOB_ORDER_OPERATIONS_V1,
                "CLOB",
                operation,
                responseBody,
                CLOB_ORDER_FIELDS,
                Set.of()
        );
    }

    private static PolymarketResponseSchemaReport validateArray(
            ObjectMapper objectMapper,
            String schemaVersion,
            String source,
            String endpoint,
            String responseBody,
            Set<String> knownFields,
            Set<String> requiredFields
    ) {
        JsonNode root = readTree(objectMapper, responseBody);
        if (root == null || !root.isArray()) {
            return incompatible(schemaVersion, source, endpoint, Set.of("$array"), Set.of(), 0);
        }

        Set<String> missing = new LinkedHashSet<>();
        Set<String> unknown = new LinkedHashSet<>();
        int itemCount = 0;
        for (JsonNode item : root) {
            itemCount++;
            if (!item.isObject()) {
                missing.add("$object");
                continue;
            }
            collectMissing(item, requiredFields, missing);
            collectUnknown(item, knownFields, unknown);
        }

        return new PolymarketResponseSchemaReport(
                schemaVersion,
                source,
                endpoint,
                itemCount,
                Set.copyOf(missing),
                Set.copyOf(unknown),
                missing.isEmpty()
        );
    }

    private static PolymarketResponseSchemaReport validateObject(
            ObjectMapper objectMapper,
            String schemaVersion,
            String source,
            String endpoint,
            String responseBody,
            Set<String> knownFields,
            Set<String> requiredFields
    ) {
        JsonNode root = readTree(objectMapper, responseBody);
        if (root == null || root.isMissingNode() || root.isNull()) {
            return new PolymarketResponseSchemaReport(
                    schemaVersion,
                    source,
                    endpoint,
                    0,
                    Set.of(),
                    Set.of(),
                    true
            );
        }
        if (!root.isObject()) {
            return incompatible(schemaVersion, source, endpoint, Set.of("$object"), Set.of(), 1);
        }

        Set<String> missing = new LinkedHashSet<>();
        Set<String> unknown = new LinkedHashSet<>();
        collectMissing(root, requiredFields, missing);
        collectUnknown(root, knownFields, unknown);

        return new PolymarketResponseSchemaReport(
                schemaVersion,
                source,
                endpoint,
                1,
                Set.copyOf(missing),
                Set.copyOf(unknown),
                missing.isEmpty()
        );
    }

    private static JsonNode readTree(ObjectMapper objectMapper, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            return null;
        }
    }

    private static void collectMissing(JsonNode item, Set<String> requiredFields, Set<String> missing) {
        for (String field : requiredFields) {
            JsonNode value = item.get(field);
            if (value == null || value.isNull() || value.asText("").isBlank()) {
                missing.add(field);
            }
        }
    }

    private static void collectUnknown(JsonNode item, Set<String> knownFields, Set<String> unknown) {
        item.fieldNames().forEachRemaining(field -> {
            if (!knownFields.contains(field)) {
                unknown.add(field);
            }
        });
    }

    private static PolymarketResponseSchemaReport incompatible(
            String schemaVersion,
            String source,
            String endpoint,
            Set<String> missingFields,
            Set<String> unknownFields,
            int itemCount
    ) {
        return new PolymarketResponseSchemaReport(
                schemaVersion,
                source,
                endpoint,
                itemCount,
                missingFields,
                unknownFields,
                false
        );
    }
}
