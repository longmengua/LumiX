/*
 * 檔案用途：Spring Data JPA repository，提供 ADL queue entry 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.AdlQueueEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdlQueueEntryEntityJpaRepository extends JpaRepository<AdlQueueEntryEntity, String> {

    List<AdlQueueEntryEntity> findAllByOrderByCreatedAtAscLiquidationIdAsc();
}
