/*
 * 檔案用途：Spring Data JPA repository，提供 hedge decision audit 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.HedgeDecisionAuditRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HedgeDecisionAuditRecordEntityJpaRepository
        extends JpaRepository<HedgeDecisionAuditRecordEntity, String> {

    List<HedgeDecisionAuditRecordEntity> findByMarketMakerIdOrderByDecidedAtDescIdDesc(
            String marketMakerId,
            Pageable pageable
    );

    List<HedgeDecisionAuditRecordEntity> findByRefIdOrderByDecidedAtAscIdAsc(String refId);
}
