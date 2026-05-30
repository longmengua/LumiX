/*
 * 檔案用途：JPA entity，保存 hedge execution worker lock。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.HedgeExecutionLock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "hedge_execution_locks")
public class HedgeExecutionLockRecord {

    @Id
    @Column(name = "lock_name", nullable = false, length = 128)
    private String lockName;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Column(name = "expires_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public HedgeExecutionLock toLock() {
        return new HedgeExecutionLock(lockName, ownerId, expiresAt, updatedAt);
    }
}
