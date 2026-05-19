/*
 * 檔案用途：JPA adapter，實作 order lifecycle projection 的 domain repository。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.OrderLifecycleProjection;
import com.example.exchange.domain.repository.OrderLifecycleProjectionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrderLifecycleProjectionStore implements OrderLifecycleProjectionStore {

    private final OrderLifecycleProjectionJpaRepository repository;

    @Override
    public void save(OrderLifecycleProjection projection) {
        repository.save(projection);
    }

    @Override
    public Optional<OrderLifecycleProjection> findByOrderId(String orderId) {
        return repository.findById(orderId);
    }

    @Override
    public List<OrderLifecycleProjection> findByUid(long uid) {
        return repository.findByUidOrderByLastEventAtDesc(uid);
    }

    @Override
    public List<OrderLifecycleProjection> findByUidAndSymbol(long uid, String symbol) {
        return repository.findByUidAndSymbolOrderByLastEventAtDesc(uid, symbol);
    }

    @Override
    public Optional<OrderLifecycleProjection> findByClientOrderId(String clientOrderId) {
        return repository.findByClientOrderId(clientOrderId);
    }
}
