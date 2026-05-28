/*
 * 檔案用途：預設 hedge venue adapter，在未設定外部 venue 前安全拒絕送單。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import org.springframework.stereotype.Component;

@Component
public class RejectingHedgeVenueAdapter implements HedgeVenueAdapter {

    @Override
    public HedgeOrderResult submit(HedgeOrderRequest request) {
        return HedgeOrderResult.rejected("HEDGE_VENUE_NOT_CONFIGURED");
    }
}
