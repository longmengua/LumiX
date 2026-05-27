/*
 * 檔案用途：Spring Data repository，查詢 durable matching command log record。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MatchingCommandLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchingCommandLogRecordJpaRepository
        extends JpaRepository<MatchingCommandLogRecord, Long> {

    List<MatchingCommandLogRecord> findBySymbolCodeAndOffsetValueGreaterThanOrderByOffsetValueAsc(
            String symbolCode,
            long offsetValue
    );

    List<MatchingCommandLogRecord> findBySymbolCodeOrderByOffsetValueAsc(String symbolCode);

    @Query("select coalesce(max(record.offsetValue), 0) from MatchingCommandLogRecord record where record.symbolCode = :symbolCode")
    long lastOffset(@Param("symbolCode") String symbolCode);
}
