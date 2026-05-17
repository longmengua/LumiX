/*
 * 檔案用途：Web 例外處理，統一業務錯誤碼與 HTTP 回應格式。
 */
package com.example.exchange.interfaces.web.exception;

public record ErrorResponse(
        int code,
        String message,
        String traceId
) {}
