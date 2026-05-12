package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.usecase.OrderUserCase;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import com.example.exchange.interfaces.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderController
 * --------------------------
 * 下單相關 REST API Controller
 * - 僅負責「接收 HTTP 請求」與「回應結果」
 * - 不包含業務邏輯（business logic），業務邏輯由 UseCase 處理
 */
@RestController                      // 標記為 REST 控制器，自動返回 JSON
@RequestMapping("/api/order")        // 所有 API 以 /api/order 開頭
@RequiredArgsConstructor             // Lombok：自動產生建構子注入 final 欄位
public class OrderController {

    // Use Case：處理下單邏輯（DDD：應用層）
    private final PlaceOrderUseCase placeOrderUseCase;

    // Order Case：查詢訂單（DDD：領域層）
    private final OrderUserCase orderUserCase;

    /**
     * 下單 API
     * -------------------
     * - URL: POST /api/order/place
     * - 輸入：PolymarketPlaceOrderRequest (JSON)
     * - 輸出：ApiResponse<String>，固定回傳 "accepted"
     *
     * @param r 下單請求參數 (uid, symbol, side, type, price, qty, leverage, marginMode)
     * @return 下單結果 (固定回覆 "accepted")
     */
    @PostMapping("/place")
    public ApiResponse<String> place(@Valid @RequestBody PlaceOrderRequest r) {
        // 將請求轉換成 Command，交給 UseCase 處理
        placeOrderUseCase.handle(r.toPlaceOrderCommand());
        return ApiResponse.ok("accepted");
    }

    /**
     * 查詢使用者「當前掛單」
     * -------------------
     * - URL: GET /api/order/open?uid=123&symbol=BTCUSDT
     * - 僅返回「尚未成交或部分成交」的訂單
     *
     * @param uid    使用者 ID (必填)
     * @param symbol 交易對 (選填，若不填則返回該用戶所有交易對的掛單)
     * @return 該用戶當前掛單清單
     */
    @GetMapping("/open")
    public ApiResponse<List<OrderInfoResponse>> openOrders(
            @RequestParam Long uid,
            @RequestParam(required = false) String symbol
    ) {
        // 查詢使用者掛單
        List<Order> orders = orderUserCase.findOpenOrders(uid, symbol);

        // 將領域物件 Order 轉換成 API 回應物件 OrderInfoResponse
        List<OrderInfoResponse> result = orders.stream()
                .map(Order::toOrderInfoResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    /**
     * 查詢使用者「所有訂單」（含歷史）
     * -------------------
     * - URL: GET /api/order/all?uid=123&symbol=BTCUSDT
     * - 包含歷史訂單與已完成/取消的訂單
     *
     * @param uid    使用者 ID (必填)
     * @param symbol 交易對 (選填，若不填則返回該用戶所有交易對的訂單)
     * @return 該用戶所有訂單清單
     */
    @GetMapping("/all")
    public ApiResponse<List<OrderInfoResponse>> allOrders(
            @RequestParam Long uid,
            @RequestParam(required = false) String symbol
    ) {
        // 查詢所有訂單（含歷史）
        List<Order> orders = orderUserCase.findAllOrders(uid, symbol);

        // 將領域物件 Order 轉換成 API 回應物件 OrderInfoResponse
        List<OrderInfoResponse> result = orders.stream()
                .map(Order::toOrderInfoResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Boolean> cancelOrder() {
        return ApiResponse.ok(true);
    }
}
