/*
 * 檔案用途：Web DTO，承接 transfer manual-review claim request。
 */
package com.example.exchange.interfaces.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferReviewClaimRequest(
        @NotNull(message = "transferId 不可為空")
        UUID transferId,

        @NotBlank(message = "owner 不可為空")
        String owner
) {
}
