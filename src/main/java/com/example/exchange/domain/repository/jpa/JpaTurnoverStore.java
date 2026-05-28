/*
 * 檔案用途：JPA adapter，實作 turnover read model store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.entity.TurnoverRecordEntity;
import com.example.exchange.domain.repository.TurnoverStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaTurnoverStore implements TurnoverStore {

    private final TurnoverRecordEntityJpaRepository repository;

    @Override
    @Transactional
    public void append(TurnoverRecord record) {
        try {
            repository.save(TurnoverRecordEntity.from(record, TurnoverStore.SCHEMA_VERSION));
        } catch (DataIntegrityViolationException ignored) {
            // Turnover 由成交事件重放產生；重複 trade_seq/order_id 視為冪等重放，不再累加。
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoverRecord> findByUid(long uid) {
        return repository.findByUidOrderByCreatedAtAscIdAsc(uid)
                .stream()
                .map(TurnoverRecordEntity::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoverRecord> findByMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) return List.of();
        return repository.findByMatchIdOrderByCreatedAtAscIdAsc(matchId)
                .stream()
                .map(TurnoverRecordEntity::toRecord)
                .toList();
    }
}
