/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class ValidationIssue {

    private final String severity;

    private final String code;

    private final String message;
    public ValidationIssue(String severity, String code, String message) {
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    public String severity() {
        return severity;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}