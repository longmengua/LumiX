/*
 * 檔案用途：real hedge venue adapter；未顯式啟用前安全拒絕實際送單。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

public class RealHedgeVenueAdapter implements HedgeVenueAdapter {

    private final boolean enabled;
    private final RealHedgeVenueSigner signer;
    private final RealHedgeVenueHttpClient httpClient;

    public RealHedgeVenueAdapter(boolean enabled, RealHedgeVenueSigner signer) {
        this(enabled, signer, null, null, null);
    }

    public RealHedgeVenueAdapter(
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
    public HedgeOrderResult submit(HedgeOrderRequest request) {
        if (!enabled) {
            return HedgeOrderResult.rejected("REAL_HEDGE_VENUE_DISABLED");
        }
        if (signer == null) {
            return HedgeOrderResult.rejected("REAL_HEDGE_VENUE_SIGNER_NOT_CONFIGURED");
        }
        SignedHedgeVenueRequest signed = signer.signSubmit(request);
        return httpClient.submit(signed, request.refId().trim());
    }
}
