package com.lumix.idempotency;

import com.lumix.common.RequestId;

import java.util.Optional;

/**
 * 冪等服務介面。
 * 只描述生命週期，不接真實儲存、鎖定或重放邏輯。
 */
public interface IdempotencyService {

    // 開始處理前先建立或標記冪等狀態，後續實作再決定由何種儲存層承接。
    IdempotencyRecord startProcessing(IdempotencyKey key, String requestHash, RequestId requestId);

    // 成功狀態只回寫語意，不代表已完成資產最終結算。
    IdempotencyRecord markSucceeded(IdempotencyKey key, String responseReference);

    // 失敗狀態也要保留 responseReference，方便後續排查與重試規則設計。
    IdempotencyRecord markFailed(IdempotencyKey key, String responseReference);

    // 查詢現有冪等紀錄，供上層決定是否要重試或直接回傳舊結果。
    Optional<IdempotencyRecord> findByKey(IdempotencyKey key);
}
