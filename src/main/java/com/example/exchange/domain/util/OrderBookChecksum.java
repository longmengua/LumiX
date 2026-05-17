/*
 * 檔案用途：領域工具，計算 order book snapshot / delta 校驗碼。
 */
package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PriceLevel;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

public final class OrderBookChecksum {

    private OrderBookChecksum() {
    }

    public static long crc32(List<PriceLevel> bids, List<PriceLevel> asks) {
        CRC32 crc32 = new CRC32();
        byte[] bytes = canonical(bids, asks).getBytes(StandardCharsets.UTF_8);
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    private static String canonical(List<PriceLevel> bids, List<PriceLevel> asks) {
        StringBuilder builder = new StringBuilder();
        int max = Math.max(size(bids), size(asks));
        for (int index = 0; index < max; index++) {
            if (index < size(bids)) {
                append(builder, bids.get(index));
            }
            if (index < size(asks)) {
                append(builder, asks.get(index));
            }
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, PriceLevel level) {
        if (!builder.isEmpty()) {
            builder.append(':');
        }
        builder.append(format(level.price()))
                .append(':')
                .append(format(level.qty()));
    }

    private static String format(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private static int size(List<PriceLevel> levels) {
        return levels == null ? 0 : levels.size();
    }
}
