/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ApiResponse
 * --------------------------
 * 統一 API 回應包裝
 * - ok: 成功標記
 * - data: 成功資料（泛型）
 * - error: 失敗物件，給前端用 stable code 做 UI 分流
 *
 * 使用範例：
 * ApiResponse.ok(data);
 * ApiResponse.fail("VALIDATION_ERROR", "參數錯誤", traceId);
 */
@Data
@AllArgsConstructor // 產生全參數建構子
public class ApiResponse<T> {

    /** 是否成功 */
    private boolean ok;

    /** 成功資料（泛型） */
    private T data;

    /** 錯誤內容（當 ok = false 時才有值） */
    private ApiError error;

    /** 建立成功回應 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 建立失敗回應；traceId 讓前端回報問題時可對應後端 log。 */
    public static <T> ApiResponse<T> fail(String code, String message, String traceId) {
        return new ApiResponse<>(false, null, new ApiError(code, message, traceId));
    }

    public record ApiError(
            String code,
            String message,
            String traceId
    ) {
    }
}
