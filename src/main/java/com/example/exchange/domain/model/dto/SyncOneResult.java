package com.example.exchange.domain.model.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncOneResult {
    private boolean success;
    private int savedCount;
    private String message;
}
