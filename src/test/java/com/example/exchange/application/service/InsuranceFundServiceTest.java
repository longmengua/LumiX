/*
 * 檔案用途：測試 insurance fund 與 ADL queue baseline 行為。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.repository.InMemoryAdlQueueStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceFundServiceTest {

    @Test
    @DisplayName("enqueueAdl 對相同 liquidationId 重送時不建立重複 queue entry")
    void enqueueAdlIsIdempotentByLiquidationId() {
        InsuranceFundService service = new InsuranceFundService();

        service.enqueueAdl("liq-1", 7, "btcusdt", "long", new BigDecimal("100"));
        service.enqueueAdl("liq-1", 7, "BTCUSDT", "LONG", new BigDecimal("100"));

        // 流程：liquidation command retry 或 replay 時，不能為同一清算缺口建立兩筆 ADL queue。
        assertThat(service.adlQueue()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.liquidationId()).isEqualTo("liq-1");
                    assertThat(entry.symbol()).isEqualTo("BTCUSDT");
                    assertThat(entry.liquidatedSide()).isEqualTo("LONG");
                    assertThat(entry.amount()).isEqualByComparingTo("100");
                });
    }

    @Test
    @DisplayName("enqueueAdl 不會用 duplicate enqueue 覆寫既有 claim owner")
    void duplicateEnqueueKeepsExistingClaimOwner() {
        InsuranceFundService service = new InsuranceFundService();
        service.enqueueAdl("liq-1", 7, "BTCUSDT", "LONG", new BigDecimal("100"));
        service.claimAdl("liq-1", "ops-1");

        service.enqueueAdl("liq-1", 7, "BTCUSDT", "LONG", new BigDecimal("100"));

        // 流程：已由 operator claim 的 queue entry 遇到 liquidation retry 時，owner guard 不能被清掉。
        assertThat(service.adlQueue()).singleElement()
                .extracting(AdlQueueEntry::status, AdlQueueEntry::owner)
                .containsExactly("CLAIMED", "ops-1");
    }

    @Test
    @DisplayName("enqueueAdl 會拒絕空 liquidationId，避免產生不可追蹤 queue entry")
    void enqueueAdlRejectsBlankLiquidationId() {
        InsuranceFundService service = new InsuranceFundService();

        assertThatThrownBy(() -> service.enqueueAdl(" ", 7, "BTCUSDT", "LONG", new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("liquidationId");
    }

    @Test
    @DisplayName("stuckAdlClaims 只列出超過 claim 時間門檻的 CLAIMED queue entry")
    void stuckAdlClaimsReturnsOnlyOldClaimedEntries() {
        InMemoryAdlQueueStore store = new InMemoryAdlQueueStore();
        InsuranceFundService service = new InsuranceFundService(store);
        store.enqueueIfAbsent(new AdlQueueEntry(
                "old-claim",
                7,
                "BTCUSDT",
                "LONG",
                new BigDecimal("100"),
                Instant.now().minus(Duration.ofHours(1)),
                "CLAIMED",
                "ops-1",
                Instant.now().minus(Duration.ofMinutes(30))
        ));
        store.enqueueIfAbsent(new AdlQueueEntry(
                "fresh-claim",
                7,
                "BTCUSDT",
                "LONG",
                new BigDecimal("100"),
                Instant.now(),
                "CLAIMED",
                "ops-2",
                Instant.now()
        ));
        service.enqueueAdl("open-entry", 7, "BTCUSDT", "LONG", new BigDecimal("100"));

        // 營運報表只應列出 claim 後卡住超過門檻的項目，不混入新 claim 或尚未 claim 的 queue。
        assertThat(service.stuckAdlClaims(Duration.ofMinutes(10)))
                .extracting(AdlQueueEntry::liquidationId)
                .containsExactly("old-claim");
    }
}
