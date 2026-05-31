/*
 * 檔案用途：real hedge venue order lookup adapter skeleton；未啟用前不對外查詢。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueOrderLookupAdapter;

import java.util.Optional;

public class RealHedgeVenueOrderLookupAdapter implements HedgeVenueOrderLookupAdapter {

    private final boolean enabled;
    private final RealHedgeVenueSigner signer;

    public RealHedgeVenueOrderLookupAdapter(boolean enabled, RealHedgeVenueSigner signer) {
        this.enabled = enabled;
        this.signer = signer;
    }

    @Override
    public Optional<HedgeOrderResult> lookupByRefId(String refId) {
        if (!enabled) {
            return Optional.empty();
        }
        if (signer == null) {
            return Optional.of(HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_LOOKUP_SIGNER_NOT_CONFIGURED"));
        }
        signer.signLookup(refId);
        return Optional.of(HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_LOOKUP_HTTP_NOT_IMPLEMENTED"));
    }
}
