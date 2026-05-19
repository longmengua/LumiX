/*
 * 檔案用途：Spring Data JPA repository，提供 account risk snapshot 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.AccountRiskSnapshotRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRiskSnapshotRecordJpaRepository
        extends JpaRepository<AccountRiskSnapshotRecord, String> {

    Optional<AccountRiskSnapshotRecord> findFirstByUidOrderByCalculatedAtDescIdDesc(long uid);

    List<AccountRiskSnapshotRecord> findByUidOrderByCalculatedAtDescIdDesc(long uid, Pageable pageable);
}
