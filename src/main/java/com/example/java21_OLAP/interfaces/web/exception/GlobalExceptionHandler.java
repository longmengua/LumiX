package com.example.java21_OLAP.interfaces.web.exception;

import com.example.java21_OLAP.interfaces.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域例外處理
 * - 將 Exception 轉為統一的 ApiResponse.fail
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handle(Exception e) {
        // 生產可加上日誌、錯誤碼、追蹤 ID
        return ApiResponse.fail(e.getMessage());
    }
}
