/*
 * 檔案用途：測試做市商 hedge fill persistence service。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.dto.HedgeVenueFillMessage;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.HedgeFillStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketMakerHedgeFillServiceTest {

    @Test
    @DisplayName("recordFill 會保存 hedge fill 並支援 market-maker、venue order、ref 查詢")
    void recordFillSupportsAuditQueries() {
        MemHedgeFillStore store = new MemHedgeFillStore();
        MarketMakerHedgeFillService service = new MarketMakerHedgeFillService(store);
        HedgeFillRecord fill = fill("venue-1", "fill-1", "trade-ref-1");

        // 流程：venue 回報 hedge fill 後保存，後續可按 market maker、venue order、ref id 對帳。
        service.recordFill(fill);

        assertThat(service.fillsByMarketMaker("mm-1", 10)).containsExactly(fill);
        assertThat(service.fillsByVenueOrder("venue-1")).containsExactly(fill);
        assertThat(service.fillsByRefId("trade-ref-1")).containsExactly(fill);
        assertThat(fill.notional()).isEqualByComparingTo("100.00000");
    }

    @Test
    @DisplayName("recordFill 會拒絕缺 venue fill id 或非正數量")
    void recordFillValidatesRequiredFieldsAndPositiveAmounts() {
        MarketMakerHedgeFillService service = new MarketMakerHedgeFillService(new MemHedgeFillStore());

        // 流程：fill id 是 venue 冪等鍵，數量價格為非正時不能進入 durable audit。
        assertThatThrownBy(() -> service.recordFill(new HedgeFillRecord(
                null,
                "mm-1",
                "BTCUSDT",
                "venue-1",
                " ",
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "USDT",
                "trade-ref-1",
                "ledger-ref-1",
                Instant.parse("2026-05-28T00:00:00Z"),
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("venue fill id");

        assertThatThrownBy(() -> service.recordFill(new HedgeFillRecord(
                null,
                "mm-1",
                "BTCUSDT",
                "venue-1",
                "fill-2",
                OrderSide.BUY,
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "USDT",
                "trade-ref-1",
                "ledger-ref-1",
                Instant.parse("2026-05-28T00:00:00Z"),
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity and price");
    }

    @Test
    @DisplayName("recordVenueFill 會將 venue message 標準化後保存")
    void recordVenueFillMapsVenueMessageIntoAuditRecord() {
        MemHedgeFillStore store = new MemHedgeFillStore();
        MarketMakerHedgeFillService service = new MarketMakerHedgeFillService(store);

        // 流程：外部 venue callback 先轉成標準 message，再由 service 正規化 symbol/ref 並保存。
        HedgeFillRecord record = service.recordVenueFill(new HedgeVenueFillMessage(
                " mm-1 ",
                "btcusdt",
                " venue-1 ",
                " fill-1 ",
                OrderSide.SELL,
                new BigDecimal("0.500"),
                new BigDecimal("101.00"),
                new BigDecimal("0.02"),
                "USDT",
                " ref-1 ",
                Instant.parse("2026-05-29T00:00:00Z")
        ));

        assertThat(record.marketMakerId()).isEqualTo("mm-1");
        assertThat(record.symbol()).isEqualTo("BTCUSDT");
        assertThat(record.venueOrderId()).isEqualTo("venue-1");
        assertThat(record.refId()).isEqualTo("ref-1");
        assertThat(service.fillsByVenueOrder("venue-1")).containsExactly(record);
    }

    @Test
    @DisplayName("recordVenueFill 對相同 venue order/fill id 重送時回傳既有 record，不重複保存")
    void recordVenueFillReplaysDuplicateVenueFillCallback() {
        MemHedgeFillStore store = new MemHedgeFillStore();
        MarketMakerHedgeFillService service = new MarketMakerHedgeFillService(store);

        HedgeFillRecord first = service.recordVenueFill(new HedgeVenueFillMessage(
                "mm-1",
                "BTCUSDT",
                "venue-1",
                "fill-1",
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "USDT",
                "ref-1",
                Instant.parse("2026-05-29T00:00:00Z")
        ));
        HedgeFillRecord replay = service.recordVenueFill(new HedgeVenueFillMessage(
                "mm-1",
                "BTCUSDT",
                "venue-1",
                "fill-1",
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "USDT",
                "ref-1",
                Instant.parse("2026-05-29T00:00:00Z")
        ));

        // 流程：外部 callback 可能因 timeout 重送；相同 venue fill key 必須 replay 既有 audit row。
        assertThat(replay).isEqualTo(first);
        assertThat(service.fillsByVenueOrder("venue-1")).containsExactly(first);
    }

    @Test
    @DisplayName("fillsByMarketMaker 拒絕無界限查詢 limit")
    void fillsByMarketMakerRejectsInvalidLimit() {
        MarketMakerHedgeFillService service = new MarketMakerHedgeFillService(new MemHedgeFillStore());

        // 流程：後台查詢不能接受 0、負數或過大 limit，避免單次 API 拉過量 audit rows。
        assertThatThrownBy(() -> service.fillsByMarketMaker("mm-1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
        assertThatThrownBy(() -> service.fillsByMarketMaker("mm-1", 501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
    }

    private static HedgeFillRecord fill(String venueOrderId, String venueFillId, String refId) {
        return new HedgeFillRecord(
                null,
                "mm-1",
                "BTCUSDT",
                venueOrderId,
                venueFillId,
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                new BigDecimal("0.01"),
                "USDT",
                refId,
                "ledger-" + venueFillId,
                Instant.parse("2026-05-28T00:00:00Z"),
                null
        );
    }

    private static class MemHedgeFillStore implements HedgeFillStore {
        private final List<HedgeFillRecord> records = new ArrayList<>();

        @Override
        public void append(HedgeFillRecord record) {
            records.add(record);
        }

        @Override
        public Optional<HedgeFillRecord> findByVenueOrderIdAndVenueFillId(String venueOrderId, String venueFillId) {
            return records.stream()
                    .filter(record -> venueOrderId.equals(record.venueOrderId())
                            && venueFillId.equals(record.venueFillId()))
                    .findFirst();
        }

        @Override
        public List<HedgeFillRecord> findByMarketMakerId(String marketMakerId, int limit) {
            return records.stream()
                    .filter(record -> marketMakerId.equals(record.marketMakerId()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<HedgeFillRecord> findByVenueOrderId(String venueOrderId) {
            return records.stream()
                    .filter(record -> venueOrderId.equals(record.venueOrderId()))
                    .toList();
        }

        @Override
        public List<HedgeFillRecord> findByRefId(String refId) {
            return records.stream()
                    .filter(record -> refId.equals(record.refId()))
                    .toList();
        }
    }
}
