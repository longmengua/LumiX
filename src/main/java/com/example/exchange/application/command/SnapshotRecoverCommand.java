/*
 * 檔案用途：應用層 Command，承載 UseCase 執行所需的輸入資料。
 */
package com.example.exchange.application.command;

/** 快照恢復指令（依 uid 恢復該使用者狀態） */
public record SnapshotRecoverCommand(long uid, Long fromSeq) {
    public SnapshotRecoverCommand(long uid) {
        this(uid, null);
    }
}
