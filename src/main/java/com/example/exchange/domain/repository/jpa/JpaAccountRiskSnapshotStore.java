/*
 * 檔案用途：JPA adapter，實作 account risk snapshot store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.entity.AccountRiskSnapshotRecord;
import com.example.exchange.domain.repository.AccountRiskSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAccountRiskSnapshotStore implements AccountRiskSnapshotStore {

    private final AccountRiskSnapshotRecordJpaRepository repository;

    @Override
    @Transactional
    public void save(AccountRiskSnapshot snapshot) {
        repository.save(AccountRiskSnapshotRecord.from(snapshot, AccountRiskSnapshotStore.SCHEMA_VERSION));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountRiskSnapshot> findLatest(long uid) {
        return repository.findFirstByUidOrderByCalculatedAtDescIdDesc(uid)
                .map(AccountRiskSnapshotRecord::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountRiskSnapshot> findByUid(long uid, int limit) {
        int normalizedLimit = Math.min(Math.max(1, limit), 500);
        return repository.findByUidOrderByCalculatedAtDescIdDesc(uid, PageRequest.of(0, normalizedLimit))
                .stream()
                .map(AccountRiskSnapshotRecord::toSnapshot)
                .toList();
    }
}
