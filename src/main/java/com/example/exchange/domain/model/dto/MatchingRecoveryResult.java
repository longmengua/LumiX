/*
 * 檔案用途：領域 DTO，承載 matching recovery orchestration 結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 單一 symbol 的 matching recovery 結果。
 *
 * @param symbolCode             normalized symbol code
 * @param recovered              是否已完成 replay recovery
 * @param snapshotFound          是否找到既有 durable snapshot
 * @param snapshotCommandOffset  replay 起點 command offset
 * @param recoveredCommandOffset recovery 後 command offset
 * @param replayedCommands       實際 replay 的 command 數量
 * @param validationValid        replay validation 是否通過
 * @param validationIssues       validation 差異列表
 * @param recoveredAt            recovery 完成時間
 */
@Data
@Builder
@Jacksonized
public class MatchingRecoveryResult {

    private final String symbolCode;

    private final boolean recovered;

    private final boolean snapshotFound;

    private final long snapshotCommandOffset;

    private final long recoveredCommandOffset;

    private final int replayedCommands;

    private final boolean validationValid;

    private final List<String> validationIssues;

    private final Instant recoveredAt;
    public MatchingRecoveryResult(String symbolCode, boolean recovered, boolean snapshotFound, long snapshotCommandOffset, long recoveredCommandOffset, int replayedCommands, boolean validationValid, List<String> validationIssues, Instant recoveredAt) {
        this.symbolCode = symbolCode;
        this.recovered = recovered;
        this.snapshotFound = snapshotFound;
        this.snapshotCommandOffset = snapshotCommandOffset;
        this.recoveredCommandOffset = recoveredCommandOffset;
        this.replayedCommands = replayedCommands;
        this.validationValid = validationValid;
        this.validationIssues = validationIssues;
        this.recoveredAt = recoveredAt;
    }

    public String symbolCode() {
        return symbolCode;
    }

    public boolean recovered() {
        return recovered;
    }

    public boolean snapshotFound() {
        return snapshotFound;
    }

    public long snapshotCommandOffset() {
        return snapshotCommandOffset;
    }

    public long recoveredCommandOffset() {
        return recoveredCommandOffset;
    }

    public int replayedCommands() {
        return replayedCommands;
    }

    public boolean validationValid() {
        return validationValid;
    }

    public List<String> validationIssues() {
        return validationIssues;
    }

    public Instant recoveredAt() {
        return recoveredAt;
    }
}