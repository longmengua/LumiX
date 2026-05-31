/*
 * 檔案用途：real hedge venue adapter skeleton；未顯式啟用前安全拒絕實際送單。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueAdapter;

public class RealHedgeVenueAdapter implements HedgeVenueAdapter {

    private final boolean enabled;
    private final RealHedgeVenueSigner signer;

    public RealHedgeVenueAdapter(boolean enabled, RealHedgeVenueSigner signer) {
        this.enabled = enabled;
        this.signer = signer;
    }

    @Override
    public HedgeOrderResult submit(HedgeOrderRequest request) {
        if (!enabled) {
            return HedgeOrderResult.rejected("REAL_HEDGE_VENUE_DISABLED");
        }
        if (signer == null) {
            return HedgeOrderResult.rejected("REAL_HEDGE_VENUE_SIGNER_NOT_CONFIGURED");
        }
        signer.signSubmit(request);
        return HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_HTTP_NOT_IMPLEMENTED");
    }
}
