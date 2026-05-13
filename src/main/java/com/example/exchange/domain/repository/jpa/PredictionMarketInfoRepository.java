package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionMarketInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Polymarket outcome market repository。
 *
 * 一筆資料代表：
 * - homeWin
 * - draw
 * - awayWin
 */
public interface PredictionMarketInfoRepository
        extends JpaRepository<PredictionMarketInfo, Long> {

    /**
     * 用 Polymarket outcome market slug 查詢。
     *
     * 注意：
     * 你的 Entity 欄位叫 marketSlug，
     * 所以 Repository method 必須叫 findByMarketSlug。
     */
    Optional<PredictionMarketInfo> findByMarketSlug(String marketSlug);

    /**
     * 查詢某場 event 的所有 outcome markets。
     */
    List<PredictionMarketInfo> findByEventSlug(String eventSlug);

    /**
     * 查詢 active 且未 closed 的 markets。
     *
     * 價格刷新用。
     */
    List<PredictionMarketInfo> findByActiveTrueAndClosedFalse();

    /**
     * 查詢某場 event 的指定 outcome。
     */
    Optional<PredictionMarketInfo> findByEventSlugAndOutcomeKey(
            String eventSlug,
            String outcomeKey
    );

    /**
     *
     * */
    List<PredictionMarketInfo> findByActiveTrueAndClosedFalseOrderByEventDateAsc();
}