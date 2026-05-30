/*
 * 檔案用途：JPA adapter，保存 trial balance 每日快照。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.TrialBalanceLine;
import com.example.exchange.domain.model.dto.TrialBalanceSnapshot;
import com.example.exchange.domain.model.entity.TrialBalanceSnapshotEntity;
import com.example.exchange.domain.repository.TrialBalanceSnapshotStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaTrialBalanceSnapshotStore implements TrialBalanceSnapshotStore {

    private static final TypeReference<List<TrialBalanceLine>> LINES_TYPE = new TypeReference<>() {
    };

    private final TrialBalanceSnapshotEntityJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TrialBalanceSnapshot save(TrialBalanceSnapshot snapshot) {
        TrialBalanceSnapshotEntity entity = repository
                .findByReportDateAndUidAndAsset(snapshot.reportDate(), snapshot.uid(), snapshot.asset())
                .orElseGet(TrialBalanceSnapshotEntity::new);
        entity.setReportDate(snapshot.reportDate());
        entity.setUid(snapshot.uid());
        entity.setAsset(snapshot.asset());
        entity.setTotalDebit(snapshot.totalDebit());
        entity.setTotalCredit(snapshot.totalCredit());
        entity.setBalanced(snapshot.balanced());
        entity.setGeneratedAt(snapshot.generatedAt());
        entity.setLinesPayload(writeLines(snapshot.lines()));
        return toSnapshot(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrialBalanceSnapshot> find(LocalDate reportDate, long uid, String asset) {
        if (reportDate == null || asset == null || asset.isBlank()) {
            return Optional.empty();
        }
        return repository.findByReportDateAndUidAndAsset(reportDate, uid, asset.trim())
                .map(this::toSnapshot);
    }

    private TrialBalanceSnapshot toSnapshot(TrialBalanceSnapshotEntity entity) {
        return new TrialBalanceSnapshot(
                entity.getReportDate(),
                entity.getUid(),
                entity.getAsset(),
                entity.getTotalDebit(),
                entity.getTotalCredit(),
                entity.getBalanced(),
                entity.getGeneratedAt(),
                readLines(entity.getLinesPayload())
        );
    }

    private String writeLines(List<TrialBalanceLine> lines) {
        try {
            return objectMapper.writeValueAsString(lines == null ? List.of() : lines);
        } catch (Exception e) {
            throw new IllegalStateException("serialize trial balance lines failed", e);
        }
    }

    private List<TrialBalanceLine> readLines(String payload) {
        try {
            return objectMapper.readValue(payload, LINES_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize trial balance lines failed", e);
        }
    }
}
