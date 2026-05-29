/*
 * 檔案用途：JPA adapter，實作 ADL forced execution idempotency / audit store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import com.example.exchange.domain.model.entity.AdlExecutionRecordEntity;
import com.example.exchange.domain.repository.AdlExecutionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAdlExecutionStore implements AdlExecutionStore {

    private final AdlExecutionRecordEntityJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AdlExecutionResult> findCompleted(String commandId) {
        if (commandId == null || commandId.isBlank()) return Optional.empty();
        return repository.findByCommandIdAndStatusIn(commandId.trim(), List.of("EXECUTED", "REJECTED"))
                .map(AdlExecutionRecordEntity::toResult);
    }

    @Override
    @Transactional
    public boolean tryStart(String commandId, AdlDeleveragingPlan plan, Instant startedAt) {
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException("ADL command id must not be blank");
        }
        String normalizedCommandId = commandId.trim();
        if (repository.existsById(normalizedCommandId)) {
            return false;
        }
        try {
            repository.save(AdlExecutionRecordEntity.started(
                    normalizedCommandId,
                    SCHEMA_VERSION,
                    plan.requestedNotional(),
                    plan.plannedNotional(),
                    plan.remainingNotional(),
                    startedAt
            ));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional
    public void complete(AdlExecutionResult result) {
        saveResult(result, "EXECUTED");
    }

    @Override
    @Transactional
    public void reject(AdlExecutionResult result) {
        saveResult(result, "REJECTED");
    }

    private void saveResult(AdlExecutionResult result, String status) {
        AdlExecutionRecordEntity entity = repository.findById(result.commandId())
                .orElseGet(() -> AdlExecutionRecordEntity.started(
                        result.commandId(),
                        SCHEMA_VERSION,
                        result.requestedNotional(),
                        result.plannedNotional(),
                        result.remainingNotional(),
                        result.executedAt()
                ));
        entity.applyResult(result, status);
        repository.save(entity);
    }
}
