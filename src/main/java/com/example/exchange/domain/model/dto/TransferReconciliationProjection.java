/*
 * 檔案用途：入金/出金 transfer 與 wallet ledger 的 reconciliation projection。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.dto.WalletTransfer;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class TransferReconciliationProjection {

    private final UUID transferId;

    private final long uid;

    private final WalletTransfer.Type type;

    private final WalletTransfer.Status status;

    private final String asset;

    private final BigDecimal transferAmount;

    private final int ledgerEntryCount;

    private final BigDecimal ledgerAmount;

    private final boolean ledgerMatched;

    private final String externalRef;

    private final String reviewOwner;

    private final String reason;
    public TransferReconciliationProjection(UUID transferId, long uid, WalletTransfer.Type type, WalletTransfer.Status status, String asset, BigDecimal transferAmount, int ledgerEntryCount, BigDecimal ledgerAmount, boolean ledgerMatched, String externalRef, String reviewOwner, String reason) {
        this.transferId = transferId;
        this.uid = uid;
        this.type = type;
        this.status = status;
        this.asset = asset;
        this.transferAmount = transferAmount;
        this.ledgerEntryCount = ledgerEntryCount;
        this.ledgerAmount = ledgerAmount;
        this.ledgerMatched = ledgerMatched;
        this.externalRef = externalRef;
        this.reviewOwner = reviewOwner;
        this.reason = reason;
    }

    public UUID transferId() {
        return transferId;
    }

    public long uid() {
        return uid;
    }

    public WalletTransfer.Type type() {
        return type;
    }

    public WalletTransfer.Status status() {
        return status;
    }

    public String asset() {
        return asset;
    }

    public BigDecimal transferAmount() {
        return transferAmount;
    }

    public int ledgerEntryCount() {
        return ledgerEntryCount;
    }

    public BigDecimal ledgerAmount() {
        return ledgerAmount;
    }

    public boolean ledgerMatched() {
        return ledgerMatched;
    }

    public String externalRef() {
        return externalRef;
    }

    public String reviewOwner() {
        return reviewOwner;
    }

    public String reason() {
        return reason;
    }
}