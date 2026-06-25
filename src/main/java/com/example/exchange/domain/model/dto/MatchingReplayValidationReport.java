/*
 * 檔案用途：領域 DTO，承載 matching replay validation 的結果報告。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 撮合 replay validation report。
 *
 * <p>Production recovery 不應只「跑完 replay」就視為成功，還需要把 replay 後狀態與
 * 期望 checkpoint 狀態比較。本 DTO 先提供可測 baseline，讓營運與測試都能看到
 * replay 是否一致，以及不一致時的具體差異。</p>
 *
 * @param symbolCode          normalized symbol code
 * @param valid               replay 結果是否與 expected snapshot 一致
 * @param startCommandOffset  replay 起點 snapshot 的 command offset
 * @param expectedCommandOffset expected snapshot 的 command offset
 * @param actualCommandOffset replay 後實際 command offset
 * @param expectedEventOffset expected snapshot 的 event offset
 * @param actualEventOffset replay 後實際 event offset
 * @param expectedMatchSequence expected snapshot 的 match sequence
 * @param actualMatchSequence replay 後實際 match sequence
 * @param issues              不一致時的差異列表
 * @param validatedAt         validation 建立時間
 */
@Data
@Builder
@Jacksonized
public class MatchingReplayValidationReport {

    private final String symbolCode;

    private final boolean valid;

    private final long startCommandOffset;

    private final long expectedCommandOffset;

    private final long actualCommandOffset;

    private final long expectedEventOffset;

    private final long actualEventOffset;

    private final long expectedMatchSequence;

    private final long actualMatchSequence;

    private final List<String> issues;

    private final Instant validatedAt;
    public MatchingReplayValidationReport(String symbolCode, boolean valid, long startCommandOffset, long expectedCommandOffset, long actualCommandOffset, long expectedEventOffset, long actualEventOffset, long expectedMatchSequence, long actualMatchSequence, List<String> issues, Instant validatedAt) {
        this.symbolCode = symbolCode;
        this.valid = valid;
        this.startCommandOffset = startCommandOffset;
        this.expectedCommandOffset = expectedCommandOffset;
        this.actualCommandOffset = actualCommandOffset;
        this.expectedEventOffset = expectedEventOffset;
        this.actualEventOffset = actualEventOffset;
        this.expectedMatchSequence = expectedMatchSequence;
        this.actualMatchSequence = actualMatchSequence;
        this.issues = issues;
        this.validatedAt = validatedAt;
    }

    public String symbolCode() {
        return symbolCode;
    }

    public boolean valid() {
        return valid;
    }

    public long startCommandOffset() {
        return startCommandOffset;
    }

    public long expectedCommandOffset() {
        return expectedCommandOffset;
    }

    public long actualCommandOffset() {
        return actualCommandOffset;
    }

    public long expectedEventOffset() {
        return expectedEventOffset;
    }

    public long actualEventOffset() {
        return actualEventOffset;
    }

    public long expectedMatchSequence() {
        return expectedMatchSequence;
    }

    public long actualMatchSequence() {
        return actualMatchSequence;
    }

    public List<String> issues() {
        return issues;
    }

    public Instant validatedAt() {
        return validatedAt;
    }
}