/*
 * 檔案用途：real hedge venue order lookup adapter；未啟用前不對外查詢。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueOrderLookupAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.util.Optional;

public class RealHedgeVenueOrderLookupAdapter implements HedgeVenueOrderLookupAdapter {

    private final boolean enabled;
    private final RealHedgeVenueSigner signer;
    private final RealHedgeVenueHttpClient httpClient;

    public RealHedgeVenueOrderLookupAdapter(boolean enabled, RealHedgeVenueSigner signer) {
        this(enabled, signer, null, null, null);
    }

    public RealHedgeVenueOrderLookupAdapter(
            boolean enabled,
            RealHedgeVenueSigner signer,
            OkHttpClient httpClient,
            String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.signer = signer;
        this.httpClient = new RealHedgeVenueHttpClient(httpClient, objectMapper, baseUrl);
    }

    @Override
    public Optional<HedgeOrderResult> lookupByRefId(String refId) {
        if (!enabled) {
            return Optional.empty();
        }
        if (signer == null) {
            return Optional.of(HedgeOrderResult.retryableRejected("REAL_HEDGE_VENUE_LOOKUP_SIGNER_NOT_CONFIGURED"));
        }
        SignedHedgeVenueRequest signed = signer.signLookup(refId);
        return httpClient.lookup(signed);
    }
}
