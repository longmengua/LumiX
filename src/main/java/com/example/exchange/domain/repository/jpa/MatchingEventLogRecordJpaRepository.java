/*
 * 檔案用途：Spring Data repository，查詢 durable matching event log record。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MatchingEventLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchingEventLogRecordJpaRepository
        extends JpaRepository<MatchingEventLogRecord, Long> {

    List<MatchingEventLogRecord> findBySymbolCodeAndOffsetValueGreaterThanOrderByOffsetValueAsc(
            String symbolCode,
            long offsetValue
    );

    List<MatchingEventLogRecord> findBySymbolCodeOrderByOffsetValueAsc(String symbolCode);

    @Query("select coalesce(max(record.offsetValue), 0) from MatchingEventLogRecord record where record.symbolCode = :symbolCode")
    long lastOffset(@Param("symbolCode") String symbolCode);
}
