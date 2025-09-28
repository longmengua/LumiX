package com.example.exchange.interfaces.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ApiResponse
 * --------------------------
 * 統一 API 回應包裝
 * - ok: 成功標記
 * - data: 成功資料（泛型）
 * - error: 失敗訊息
 *
 * 使用範例：
 * ApiResponse.ok(data);
 * ApiResponse.fail("錯誤訊息");
 */
@Data
@AllArgsConstructor // 產生全參數建構子
public class ApiResponse<T> {

    /** 是否成功 */
    private boolean ok;

    /** 成功資料（泛型） */
    private T data;

    /** 錯誤訊息（當 ok = false 時才有值） */
    private String error;

    /** 建立成功回應 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 建立失敗回應 */
    public static <T> ApiResponse<T> fail(String err) {
        return new ApiResponse<>(false, null, err);
    }
}
