/*
 * 檔案用途：Spring Data JPA repository，提供 market-data trade tape 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketDataTradeTapeRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MarketDataTradeTapeRecordJpaRepository
        extends JpaRepository<MarketDataTradeTapeRecord, Long> {

    List<MarketDataTradeTapeRecord> findBySymbolOrderByTradeTsDescIdDesc(String symbol, Pageable pageable);

    @Query("""
            select record from MarketDataTradeTapeRecord record
            where record.symbol = :symbol
              and (
                    record.tradeTs > :afterTs
                    or (
                        record.tradeTs = :afterTs
                        and (:afterMatchId is null or record.matchId > :afterMatchId)
                    )
              )
            order by record.tradeTs asc, record.matchId asc, record.id asc
            """)
    List<MarketDataTradeTapeRecord> findAfterCursor(
            @Param("symbol") String symbol,
            @Param("afterTs") Instant afterTs,
            @Param("afterMatchId") String afterMatchId,
            Pageable pageable
    );

    @Query("""
            select record from MarketDataTradeTapeRecord record
            where record.symbol = :symbol
              and (
                    record.tradeTs < :beforeTs
                    or (
                        record.tradeTs = :beforeTs
                        and (:beforeMatchId is null or record.matchId < :beforeMatchId)
                    )
              )
            order by record.tradeTs desc, record.matchId desc, record.id desc
            """)
    List<MarketDataTradeTapeRecord> findBeforeCursor(
            @Param("symbol") String symbol,
            @Param("beforeTs") Instant beforeTs,
            @Param("beforeMatchId") String beforeMatchId,
            Pageable pageable
    );

    List<MarketDataTradeTapeRecord> findByMatchIdOrderByTradeTsAscIdAsc(String matchId);

    long deleteByTradeTsBefore(Instant cutoff);
}
