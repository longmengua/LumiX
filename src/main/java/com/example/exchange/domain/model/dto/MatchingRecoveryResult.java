/*
 * 檔案用途：領域 DTO，承載 matching recovery orchestration 結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

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
public record MatchingRecoveryResult(
        String symbolCode,
        boolean recovered,
        boolean snapshotFound,
        long snapshotCommandOffset,
        long recoveredCommandOffset,
        int replayedCommands,
        boolean validationValid,
        List<String> validationIssues,
        Instant recoveredAt
) {
}
