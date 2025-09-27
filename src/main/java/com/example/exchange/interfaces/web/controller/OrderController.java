package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 下單相關 REST API
 * - 只負責接收/回應，不放商業邏輯
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final PlaceOrderUseCase usecase;

    public OrderController(PlaceOrderUseCase usecase) {
        this.usecase = usecase;
    }

    @PostMapping("/place")
    public ApiResponse<String> place(@Valid @RequestBody PlaceOrderRequest r) {
        usecase.handle(new PlaceOrderCommand(
                r.uid(), r.symbol(), r.side(), r.type(), r.price(), r.qty(), r.leverage(), r.marginMode()
        ));
        return ApiResponse.ok("accepted");
    }
}
