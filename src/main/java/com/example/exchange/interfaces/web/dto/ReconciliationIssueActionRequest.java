/*
 * 檔案用途：REST request DTO，承載 reconciliation issue workflow 操作人。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReconciliationIssueActionRequest(
        @NotBlank String owner
) {
}
