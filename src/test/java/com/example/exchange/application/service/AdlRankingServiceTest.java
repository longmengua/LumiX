/*
 * 檔案用途：測試 ADL queue deterministic ranking。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlRankedPosition;
import com.example.exchange.domain.model.dto.AdlRankingCandidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADL ranking tests。
 *
 * <p>固定 production ADL queue 的排序語義，避免 forced deleveraging 在 replay 或不同節點
 * 因 tie-breaker 不明確而出現不同結果。</p>
 */
class AdlRankingServiceTest {

    private final AdlRankingService service = new AdlRankingService();

    @Test
    @DisplayName("ADL ranking 依獲利率、有效槓桿、名義價值與 uid 穩定排序")
    /**
     * 流程：建立多個獲利候選倉位 -> ranking -> 驗證 profit rate 優先，其次 leverage/notional/uid。
     */
    void ranksByProfitRateEffectiveLeverageNotionalAndUid() {
        List<AdlRankedPosition> ranked = service.rank(List.of(
                candidate(30, "BTCUSDT", "1", "100", "120", "20", "10"),
                candidate(10, "BTCUSDT", "1", "100", "130", "30", "10"),
                candidate(20, "BTCUSDT", "2", "100", "130", "40", "10"),
                candidate(5, "BTCUSDT", "1", "100", "130", "40", "10")
        ));

        assertThat(ranked).extracting(AdlRankedPosition::uid)
                .containsExactly(20L, 10L, 5L, 30L);
        assertThat(ranked).extracting(AdlRankedPosition::rank)
                .containsExactly(1, 2, 3, 4);
        assertThat(ranked.getFirst().profitRate()).isEqualByComparingTo("0.300000000000000000");
        assertThat(ranked.getFirst().effectiveLeverage()).isEqualByComparingTo("6.500000000000000000");
    }

    @Test
    @DisplayName("ADL ranking 會排除虧損、零倉位與非法價格候選")
    /**
     * 流程：混入虧損、零倉位、非法價格與有效 short 獲利倉位 -> 只保留可 ADL 的高獲利候選。
     */
    void excludesLossZeroAndInvalidCandidates() {
        List<AdlRankedPosition> ranked = service.rank(List.of(
                candidate(1, "BTCUSDT", "1", "100", "90", "10", "10"),
                candidate(2, "BTCUSDT", "0", "100", "120", "10", "10"),
                candidate(3, "BTCUSDT", "1", "0", "120", "10", "10"),
                candidate(4, "BTCUSDT", "-1", "100", "80", "10", "10")
        ));

        assertThat(ranked).singleElement()
                .extracting(AdlRankedPosition::uid)
                .isEqualTo(4L);
        assertThat(ranked.getFirst().profitRate()).isEqualByComparingTo("0.200000000000000000");
    }

    /**
     * 建立 ADL ranking candidate，讓測試案例只描述排序必要欄位。
     */
    private static AdlRankingCandidate candidate(
            long uid,
            String symbol,
            String qty,
            String entryPrice,
            String markPrice,
            String margin,
            String leverage
    ) {
        return new AdlRankingCandidate(
                uid,
                symbol,
                new BigDecimal(qty),
                new BigDecimal(entryPrice),
                new BigDecimal(markPrice),
                new BigDecimal(margin),
                new BigDecimal(leverage)
        );
    }
}
