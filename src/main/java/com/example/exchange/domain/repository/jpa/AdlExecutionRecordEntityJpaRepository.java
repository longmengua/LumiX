/*
 * 檔案用途：Spring Data JPA repository，提供 ADL forced execution record 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.AdlExecutionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdlExecutionRecordEntityJpaRepository
        extends JpaRepository<AdlExecutionRecordEntity, String> {

    Optional<AdlExecutionRecordEntity> findByCommandIdAndStatusIn(String commandId, Iterable<String> statuses);
}
