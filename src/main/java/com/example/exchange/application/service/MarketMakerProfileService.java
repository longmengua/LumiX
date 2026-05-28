/*
 * 檔案用途：應用服務，管理做市商 profile 與 per-symbol risk limits。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.repository.MarketMakerProfileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketMakerProfileService {

    private final MarketMakerProfileStore profileStore;

    @Transactional
    public MarketMakerProfile save(MarketMakerProfile profile) {
        validate(profile);
        profileStore.save(profile);
        return profile;
    }

    @Transactional(readOnly = true)
    public Optional<MarketMakerProfile> findByMarketMakerId(String marketMakerId) {
        return profileStore.findByMarketMakerId(marketMakerId);
    }

    @Transactional(readOnly = true)
    public Optional<MarketMakerProfile> findByUid(long uid) {
        return profileStore.findByUid(uid);
    }

    @Transactional(readOnly = true)
    public List<MarketMakerProfile> enabledProfiles() {
        return profileStore.findEnabled();
    }

    private static void validate(MarketMakerProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("market maker profile cannot be null");
        }
        if (profile.marketMakerId() == null || profile.marketMakerId().isBlank()) {
            throw new IllegalArgumentException("market maker id is required");
        }
        if (profile.uid() <= 0) {
            throw new IllegalArgumentException("market maker uid must be positive");
        }
        profile.riskLimits().forEach(limit -> {
            if (limit.symbol() == null || limit.symbol().isBlank()) {
                throw new IllegalArgumentException("risk limit symbol is required");
            }
        });
    }
}
