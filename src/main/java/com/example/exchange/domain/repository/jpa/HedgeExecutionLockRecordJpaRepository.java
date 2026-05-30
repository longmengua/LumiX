/*
 * 檔案用途：Spring Data repository，鎖定 hedge execution lock row。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.HedgeExecutionLockRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HedgeExecutionLockRecordJpaRepository extends JpaRepository<HedgeExecutionLockRecord, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lock from HedgeExecutionLockRecord lock where lock.lockName = :lockName")
    Optional<HedgeExecutionLockRecord> findLocked(@Param("lockName") String lockName);
}
