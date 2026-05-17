/*
 * 檔案用途：領域工具，封裝簽名、JSON 處理或文字解析等純技術細節。
 */
package com.example.exchange.domain.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Gamma JSON 工具。
 *
 * Gamma 很多欄位是 stringified JSON array：
 * "[\"0.645\",\"0.355\"]"
 */
public class PredictionJsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PredictionJsonUtils() {
    }

    public static List<String> safeStringArray(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return MAPPER.readValue(
                    value,
                    new TypeReference<List<String>>() {
                    }
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static Double safeDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }
}
