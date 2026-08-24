package com.lumix.recovery;
/** restore/replay 前置狀態；非 READY 只能維持 read-only 與人工升級。 */
public enum RecoveryReadiness { READY, MANIFEST_MISMATCH, REPLAY_MISMATCH, HUMAN_APPROVAL_MISSING }
