/*
 * 檔案用途：入金/出金 transfer 與 wallet ledger 的 reconciliation projection。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.entity.WalletTransfer;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferReconciliationProjection(
        UUID transferId,
        long uid,
        WalletTransfer.Type type,
        WalletTransfer.Status status,
        String asset,
        BigDecimal transferAmount,
        int ledgerEntryCount,
        BigDecimal ledgerAmount,
        boolean ledgerMatched,
        String externalRef,
        String reviewOwner,
        String reason
) {
}
