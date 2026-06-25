/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 錢包帳務流水。每筆流水必須 debit total == credit total。
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletLedgerEntry {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private long uid;
    private String asset;
    private String reason;
    private String refId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private List<WalletLedgerPosting> postings;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isBalanced() {
        BigDecimal debit = postings == null
                ? BigDecimal.ZERO
                : postings.stream().map(WalletLedgerPosting::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = postings == null
                ? BigDecimal.ZERO
                : postings.stream().map(WalletLedgerPosting::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return debit.compareTo(credit) == 0;
    }
}
