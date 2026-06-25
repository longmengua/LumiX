/*
 * 檔案用途：Spring Data JPA repository，提供 live position SQL mirror 查詢入口。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PositionLifecycleProjection;
import com.example.exchange.domain.model.dto.PositionLifecycleProjectionId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface PositionLifecycleProjectionJpaRepository
        extends JpaRepository<PositionLifecycleProjection, PositionLifecycleProjectionId> {

    List<PositionLifecycleProjection> findBySymbolAndQtyNotOrderByUpdatedAtAsc(
            String symbol,
            BigDecimal qty,
            Pageable pageable
    );

    List<PositionLifecycleProjection> findByUidOrderByUpdatedAtDesc(long uid, Pageable pageable);
}
