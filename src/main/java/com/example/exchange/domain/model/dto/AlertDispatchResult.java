/*
 * 檔案用途：描述 alert backend dispatch 結果，讓呼叫端可記錄送達、略過或失敗而不影響交易狀態。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AlertDispatchResult {

    private final AlertDispatchStatus status;

    private final int statusCode;

    private final String message;
    public AlertDispatchResult(AlertDispatchStatus status, int statusCode, String message) {
        this.status = status;
        this.statusCode = statusCode;
        this.message = message;
    }

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

    public AlertDispatchStatus status() {
        return status;
    }

    public int statusCode() {
        return statusCode;
    }

    public String message() {
        return message;
    }
}