/*
 * 檔案用途：領域服務契約，抽象外部 hedge venue 送單能力。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;

public interface HedgeVenueAdapter {

    HedgeOrderResult submit(HedgeOrderRequest request);
}
