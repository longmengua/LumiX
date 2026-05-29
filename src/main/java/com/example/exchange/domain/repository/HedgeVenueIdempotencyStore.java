/*
 * 檔案用途：Repository 介面，定義 hedge venue submit idempotency claim/result 儲存契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyRecord;

import java.util.Optional;

public interface HedgeVenueIdempotencyStore {

    Optional<HedgeVenueIdempotencyRecord> find(String refId);

    boolean claim(String refId, String fingerprint);

    HedgeVenueIdempotencyRecord complete(String refId, String fingerprint, HedgeOrderResult result);
}
