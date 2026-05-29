/*
 * 檔案用途：Spring Data JPA repository，提供體驗金 grant 批次查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.BonusCreditGrantRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BonusCreditGrantRecordJpaRepository extends JpaRepository<BonusCreditGrantRecord, String> {

    @Query("""
            select grant from BonusCreditGrantRecord grant
            where grant.uid = :uid
              and grant.asset = :asset
              and grant.status = :status
            order by
              case when grant.expiresAt is null then 1 else 0 end,
              grant.expiresAt asc,
              grant.grantedAt asc,
              grant.id asc
            """)
    List<BonusCreditGrantRecord> findActiveForConsumption(
            @Param("uid") long uid,
            @Param("asset") String asset,
            @Param("status") String status
    );

    List<BonusCreditGrantRecord> findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAscIdAsc(
            String status,
            Instant expiresAt,
            Pageable pageable
    );

    List<BonusCreditGrantRecord> findByUidOrderByGrantedAtAscIdAsc(long uid);

    List<BonusCreditGrantRecord> findByCampaignIdOrderByGrantedAtAscIdAsc(String campaignId);
}
