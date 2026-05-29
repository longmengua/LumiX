/*
 * 檔案用途：測試 ADL queue store contract，確保 durable adapter 前的共同語意穩定。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAdlQueueStoreTest {

    @Test
    @DisplayName("ADL queue store 以 liquidationId 冪等 enqueue，避免 retry 產生重複 shortfall")
    void enqueueIfAbsentIsIdempotentByLiquidationId() {
        InMemoryAdlQueueStore store = new InMemoryAdlQueueStore();

        assertThat(store.enqueueIfAbsent(entry("liq-1", "OPEN", "", null))).isTrue();
        assertThat(store.enqueueIfAbsent(entry("liq-1", "OPEN", "", null))).isFalse();

        assertThat(store.list()).singleElement()
                .extracting(AdlQueueEntry::liquidationId, AdlQueueEntry::amount)
                .containsExactly("liq-1", new BigDecimal("100"));
    }

    @Test
    @DisplayName("ADL queue store partial retry 只更新剩餘金額並保留 operator claim")
    void updateRemainingKeepsClaimState() {
        InMemoryAdlQueueStore store = new InMemoryAdlQueueStore();
        store.enqueueIfAbsent(entry("liq-1", "OPEN", "", null));
        store.claim("liq-1", "ops-1");

        AdlQueueEntry updated = store.updateRemaining("liq-1", new BigDecimal("30"));

        assertThat(updated.amount()).isEqualByComparingTo("30");
        assertThat(updated.status()).isEqualTo("CLAIMED");
        assertThat(updated.owner()).isEqualTo("ops-1");
    }

    @Test
    @DisplayName("ADL queue store 不允許其他 operator release 已 claim 的 entry")
    void releaseRejectsNonOwner() {
        InMemoryAdlQueueStore store = new InMemoryAdlQueueStore();
        store.enqueueIfAbsent(entry("liq-1", "OPEN", "", null));
        store.claim("liq-1", "ops-1");

        assertThatThrownBy(() -> store.release("liq-1", "ops-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ops-1");
    }

    @Test
    @DisplayName("ADL queue store 將零剩餘金額視為完成並移除 queue entry")
    void updateRemainingWithZeroCompletesEntry() {
        InMemoryAdlQueueStore store = new InMemoryAdlQueueStore();
        store.enqueueIfAbsent(entry("liq-1", "OPEN", "", null));

        AdlQueueEntry updated = store.updateRemaining("liq-1", BigDecimal.ZERO);

        assertThat(updated).isNull();
        assertThat(store.list()).isEmpty();
    }

    private static AdlQueueEntry entry(String liquidationId, String status, String owner, Instant claimedAt) {
        return new AdlQueueEntry(
                liquidationId,
                7,
                "BTCUSDT",
                "LONG",
                new BigDecimal("100"),
                Instant.parse("2026-05-30T00:00:00Z"),
                status,
                owner,
                claimedAt
        );
    }
}
