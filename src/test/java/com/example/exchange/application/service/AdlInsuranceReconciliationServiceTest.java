/*
 * 檔案用途：測試 ADL queue、insurance fund 與 liquidated position coverage 對帳。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlInsuranceReconciliationReport;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.PositionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdlInsuranceReconciliationServiceTest {

    @Test
    @DisplayName("reconcile 會找出 ADL queue 與 liquidated position coverage 差異")
    void reconcileReportsQueueCoverageMismatches() {
        InsuranceFundService insuranceFundService = new InsuranceFundService();
        MemPositionRepository positionRepository = new MemPositionRepository();
        AdlInsuranceReconciliationService service =
                new AdlInsuranceReconciliationService(insuranceFundService, positionRepository);
        insuranceFundService.enqueueAdl("liq-ok", 7, "BTCUSDT", "LONG", new BigDecimal("100"));
        insuranceFundService.enqueueAdl("liq-missing", 8, "BTCUSDT", "LONG", new BigDecimal("50"));
        insuranceFundService.enqueueAdl("liq-exceeds", 9, "BTCUSDT", "LONG", new BigDecimal("200"));
        positionRepository.save(position(7, "100", "25"));
        positionRepository.save(position(9, "100", "10"));

        // 流程：ADL queue 是待承接 shortfall，position coverage 是 liquidation 記錄；兩者不一致要進營運報告。
        AdlInsuranceReconciliationReport report = service.reconcile("usdt");

        assertThat(report.asset()).isEqualTo("USDT");
        assertThat(report.openAdlQueueCount()).isEqualTo(3);
        assertThat(report.openAdlQueueAmount()).isEqualByComparingTo("350");
        assertThat(report.issueCount()).isEqualTo(2);
        assertThat(report.issues()).extracting("reason")
                .containsExactly("MISSING_LIQUIDATED_POSITION", "QUEUE_EXCEEDS_POSITION_ADL_COVERAGE");
    }

    private static Position position(long uid, String adlCovered, String insuranceCovered) {
        Position position = Position.builder()
                .uid(uid)
                .symbol(Symbol.builder().base("BTC").quote("USDT").priceScale(1).qtyScale(3).build())
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(BigDecimal.ZERO)
                .entryPrice(BigDecimal.ZERO)
                .margin(BigDecimal.ZERO)
                .build();
        position.addAdlCovered(new BigDecimal(adlCovered));
        position.addInsuranceFundCovered(new BigDecimal(insuranceCovered));
        return position;
    }

    private static class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream()
                    .filter(position -> position.getUid() == uid)
                    .toList();
        }

        @Override
        public List<Position> findOpenPositions() {
            return new ArrayList<>(positions.values());
        }

        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }
}
