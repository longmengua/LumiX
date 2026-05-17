/*
 * 檔案用途：REST Controller，暴露營運觀測與操作 API。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.OperationalMetricsService;
import com.example.exchange.domain.model.dto.OperationalMetricsSnapshot;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OperationsController {

    private final OperationalMetricsService operationalMetricsService;

    @GetMapping("/metrics")
    public ApiResponse<OperationalMetricsSnapshot> metrics() {
        return ApiResponse.ok(operationalMetricsService.snapshot());
    }
}
