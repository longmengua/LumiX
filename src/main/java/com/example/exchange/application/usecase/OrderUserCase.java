/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderUserCase {

    // Order: 查詢訂單
    private final OrderRepository orderRepository;

    // 查詢使用者掛單
    public List<Order> findOpenOrders(Long uid, String symbol) {
        return orderRepository.findOpenOrders(uid, symbol);
    }

    // 查詢所有訂單（含歷史）
    public List<Order> findAllOrders(Long uid, String symbol) {
        return orderRepository.findAllOrders(uid, symbol);
    }
}
