/*
 * 檔案用途：Spring Data JPA repository，提供 hedge fill audit 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.HedgeFillRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HedgeFillRecordEntityJpaRepository extends JpaRepository<HedgeFillRecordEntity, String> {

    List<HedgeFillRecordEntity> findByMarketMakerIdOrderByFilledAtDescIdDesc(
            String marketMakerId,
            Pageable pageable
    );

    List<HedgeFillRecordEntity> findByVenueOrderIdOrderByFilledAtAscIdAsc(String venueOrderId);

    Optional<HedgeFillRecordEntity> findByVenueOrderIdAndVenueFillId(String venueOrderId, String venueFillId);

    List<HedgeFillRecordEntity> findByRefIdOrderByFilledAtAscIdAsc(String refId);
}
