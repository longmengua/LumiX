/*
 * 檔案用途：領域服務契約，抽象 hedge venue order outcome lookup 能力。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.HedgeOrderResult;

import java.util.Optional;

public interface HedgeVenueOrderLookupAdapter {

    Optional<HedgeOrderResult> lookupByRefId(String refId);
}
