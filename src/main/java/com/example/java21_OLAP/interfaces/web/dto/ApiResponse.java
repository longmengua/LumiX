package com.example.java21_OLAP.interfaces.web.dto;

/**
 * 統一 API 回應包裝
 * - ok: 成功標記
 * - data: 成功資料（泛型）
 * - error: 失敗訊息
 */
public record ApiResponse<T>(
        boolean ok,
        T data,
        String error
) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null); }
    public static <T> ApiResponse<T> fail(String err) { return new ApiResponse<>(false, null, err); }
}
