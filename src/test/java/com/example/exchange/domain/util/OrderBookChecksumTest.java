/*
 * 檔案用途：測試 order book checksum 的穩定性。
 */
package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PriceLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBookChecksumTest {

    @Test
    void crc32IsStableAcrossEquivalentDecimalScales() {
        long left = OrderBookChecksum.crc32(
                List.of(new PriceLevel(new BigDecimal("100.00"), new BigDecimal("1.000"))),
                List.of(new PriceLevel(new BigDecimal("101.0"), new BigDecimal("2.00")))
        );
        long right = OrderBookChecksum.crc32(
                List.of(new PriceLevel(new BigDecimal("100"), new BigDecimal("1"))),
                List.of(new PriceLevel(new BigDecimal("101.00"), new BigDecimal("2.000")))
        );

        assertThat(left).isEqualTo(right);
        assertThat(left).isPositive();
    }
}
