/*
 * 檔案用途：Spring Data JPA repository，提供 turnover read model 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.TurnoverRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurnoverRecordEntityJpaRepository extends JpaRepository<TurnoverRecordEntity, String> {

    List<TurnoverRecordEntity> findByUidOrderByCreatedAtAscIdAsc(long uid);

    List<TurnoverRecordEntity> findByMatchIdOrderByCreatedAtAscIdAsc(String matchId);
}
