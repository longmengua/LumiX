/*
 * 檔案用途：JPA adapter，實作 market-data durable sequence checkpoint store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MarketDataSequenceCheckpoint;
import com.example.exchange.domain.model.entity.MarketDataSequenceCheckpointRecord;
import com.example.exchange.domain.repository.MarketDataSequenceCheckpointStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMarketDataSequenceCheckpointStore implements MarketDataSequenceCheckpointStore {

    private final MarketDataSequenceCheckpointRecordJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketDataSequenceCheckpoint> find(String symbol, String stream) {
        MarketDataSequenceCheckpointRecord.Key key = new MarketDataSequenceCheckpointRecord.Key();
        key.setSymbol(symbol);
        key.setStream(stream);
        return repository.findById(key)
                .map(MarketDataSequenceCheckpointRecord::toCheckpoint);
    }

    @Override
    @Transactional
    public MarketDataSequenceCheckpoint save(MarketDataSequenceCheckpoint checkpoint) {
        return repository.save(MarketDataSequenceCheckpointRecord.from(checkpoint)).toCheckpoint();
    }
}
