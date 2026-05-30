/*
 * 檔案用途：預設 hedge venue lookup adapter，在未接真實 venue 前不回填任何 outcome。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueOrderLookupAdapter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoopHedgeVenueOrderLookupAdapter implements HedgeVenueOrderLookupAdapter {

    @Override
    public Optional<HedgeOrderResult> lookupByRefId(String refId) {
        return Optional.empty();
    }
}
