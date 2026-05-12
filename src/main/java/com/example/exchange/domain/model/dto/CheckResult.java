package com.example.exchange.domain.model.dto;

import lombok.*;

/**
 * 單一 key 檢查結果。
 *
 * 用途：
 * - 判斷 sync 是否成功
 * - 記錄缺少哪些 outcome
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
@Builder
public class CheckResult {

    /**
     * 是否成功。
     */
    private boolean success;

    /**
     * 錯誤訊息 / 缺少的 outcome。
     */
    private String message;
}