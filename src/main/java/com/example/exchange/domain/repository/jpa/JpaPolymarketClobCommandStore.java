/*
 * 檔案用途：JPA adapter，實作 Polymarket CLOB command idempotency claim/result store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.PolymarketClobCommandRecord;
import com.example.exchange.domain.model.entity.PolymarketClobCommandRecordEntity;
import com.example.exchange.domain.repository.PolymarketClobCommandStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaPolymarketClobCommandStore implements PolymarketClobCommandStore {

    private final PolymarketClobCommandRecordRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PolymarketClobCommandRecord> find(String commandId) {
        if (commandId == null || commandId.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(commandId.trim())
                .map(PolymarketClobCommandRecordEntity::toRecord);
    }

    @Override
    @Transactional
    public boolean claim(
            String commandId,
            String commandType,
            String internalOrderId,
            String fingerprint
    ) {
        String normalizedCommandId =
                commandId.trim();
        if (repository.existsById(normalizedCommandId)) {
            return false;
        }

        try {
            repository.save(PolymarketClobCommandRecordEntity.claimed(
                    normalizedCommandId,
                    commandType,
                    internalOrderId,
                    fingerprint,
                    Instant.now()
            ));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional
    public PolymarketClobCommandRecord complete(
            String commandId,
            String resultStatus,
            String lastError
    ) {
        PolymarketClobCommandRecordEntity entity =
                repository.findById(commandId.trim())
                        .orElseThrow(() -> new IllegalStateException("polymarket CLOB command claim missing: " + commandId));
        entity.setCompleted(true);
        entity.setResultStatus(resultStatus);
        entity.setLastError(lastError);
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity).toRecord();
    }
}
