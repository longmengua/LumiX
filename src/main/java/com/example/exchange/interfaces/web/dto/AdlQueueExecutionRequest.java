/*
 * 檔案用途：Web DTO，承載 ADL queue execution request。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AdlQueueExecutionRequest(
        @NotBlank String commandId,
        String operatorId
) {
}
