/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

public record ValidationIssue(
        String severity,
        String code,
        String message
) {}
