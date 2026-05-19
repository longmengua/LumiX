/*
 * 檔案用途：Spring Data JPA repository，提供 order lifecycle projection 的資料庫存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.OrderLifecycleProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderLifecycleProjectionJpaRepository
        extends JpaRepository<OrderLifecycleProjection, String> {

    List<OrderLifecycleProjection> findByUidOrderByLastEventAtDesc(Long uid);

    List<OrderLifecycleProjection> findByUidAndSymbolOrderByLastEventAtDesc(Long uid, String symbol);

    Optional<OrderLifecycleProjection> findByClientOrderId(String clientOrderId);
}
