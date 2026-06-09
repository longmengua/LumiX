/*
 * 檔案用途：描述 alert backend dispatch 結果，讓呼叫端可記錄送達、略過或失敗而不影響交易狀態。
 */
package com.example.exchange.domain.model.dto;

public record AlertDispatchResult(
        AlertDispatchStatus status,
        int statusCode,
        String message
) {

    public static AlertDispatchResult delivered(int statusCode) {
        return new AlertDispatchResult(AlertDispatchStatus.DELIVERED, statusCode, "DELIVERED");
    }

    public static AlertDispatchResult skipped(String message) {
        return new AlertDispatchResult(AlertDispatchStatus.SKIPPED, 0, message);
    }

    public static AlertDispatchResult failed(int statusCode, String message) {
        return new AlertDispatchResult(AlertDispatchStatus.FAILED, statusCode, message);
    }

    public enum AlertDispatchStatus {
        DELIVERED,
        SKIPPED,
        FAILED
    }
}
