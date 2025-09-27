package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.Order;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提供查詢使用者訂單的 API
 */
@RestController
@RequestMapping("/api/order")
public class OrderQueryController {

    private final OrderRepository orderRepo;

    public OrderQueryController(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    /** 查詢使用者「當前掛單」 */
    @GetMapping("/open")
    public ApiResponse<List<OrderInfoResponse>> openOrders(@RequestParam Long uid,
                                                           @RequestParam(required = false) String symbol) {
        List<Order> orders = orderRepo.findOpenOrders(uid, symbol);

        List<OrderInfoResponse> result = orders.stream()
                .map(o -> new OrderInfoResponse(
                        o.getId().toString(),
                        o.getUid(),
                        o.getSymbol().code(),
                        o.getSide(),
                        o.getType(),
                        o.getPrice(),
                        o.getQty(),
                        o.getStatus().name(),
                        o.getCtime()
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    /** 查詢使用者「所有訂單」（含歷史） */
    @GetMapping("/all")
    public ApiResponse<List<OrderInfoResponse>> allOrders(@RequestParam Long uid,
                                                          @RequestParam(required = false) String symbol) {
        List<Order> orders = orderRepo.findAllOrders(uid, symbol);

        List<OrderInfoResponse> result = orders.stream()
                .map(o -> new OrderInfoResponse(
                        o.getId().toString(),
                        o.getUid(),
                        o.getSymbol().code(),
                        o.getSide(),
                        o.getType(),
                        o.getPrice(),
                        o.getQty(),
                        o.getStatus().name(),
                        o.getCtime()
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }
}
