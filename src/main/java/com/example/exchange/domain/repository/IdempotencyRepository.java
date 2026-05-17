/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import java.time.Instant;

public interface IdempotencyRepository {

    boolean insertIfAbsent(String key, Instant expiresAt);

    boolean exists(String key);
}
