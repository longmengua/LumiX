/*
 * 檔案用途：保存 Polymarket CLOB effectful command 的 idempotency 狀態。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class PolymarketClobCommandRecord {

    private final String commandId;

    private final String commandType;

    private final String internalOrderId;

    private final String fingerprint;

    private final boolean completed;

    private final String resultStatus;

    private final String lastError;
    public PolymarketClobCommandRecord(String commandId, String commandType, String internalOrderId, String fingerprint, boolean completed, String resultStatus, String lastError) {
        this.commandId = commandId;
        this.commandType = commandType;
        this.internalOrderId = internalOrderId;
        this.fingerprint = fingerprint;
        this.completed = completed;
        this.resultStatus = resultStatus;
        this.lastError = lastError;
    }

    public String commandId() {
        return commandId;
    }

    public String commandType() {
        return commandType;
    }

    public String internalOrderId() {
        return internalOrderId;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public boolean completed() {
        return completed;
    }

    public String resultStatus() {
        return resultStatus;
    }

    public String lastError() {
        return lastError;
    }
}