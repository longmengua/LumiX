/*
 * 檔案用途：測試 MarketMakerProfileService 的 profile 保存與查詢 baseline。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.repository.MarketMakerProfileStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketMakerProfileServiceTest {

    @Test
    @DisplayName("save 會保存 profile/risk limits，並可依 id、uid、enabled 查詢")
    void saveAndQueryMarketMakerProfile() {
        MemMarketMakerProfileStore store = new MemMarketMakerProfileStore();
        MarketMakerProfileService service = new MarketMakerProfileService(store);
        MarketMakerProfile profile = profile("mm-store-1", 9201, true);

        // 流程：保存做市商 profile，後續 hedging/quote service 可依 id 或 uid 載入同一份風控限制。
        service.save(profile);

        assertThat(service.findByMarketMakerId("mm-store-1")).contains(profile);
        assertThat(service.findByUid(9201)).contains(profile);
        assertThat(service.enabledProfiles()).containsExactly(profile);
    }

    @Test
    @DisplayName("save 會拒絕沒有 symbol 的 risk limit")
    void saveRejectsRiskLimitWithoutSymbol() {
        MarketMakerProfileService service = new MarketMakerProfileService(new MemMarketMakerProfileStore());
        MarketMakerProfile profile = new MarketMakerProfile(
                "mm-invalid",
                9202,
                true,
                List.of(new MarketMakerRiskLimit(
                        " ",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false
                ))
        );

        // 流程：risk limit 沒有 symbol 會讓後續 quote/hedge 找不到限制，必須在保存時拒絕。
        assertThatThrownBy(() -> service.save(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("risk limit symbol");
    }

    private static MarketMakerProfile profile(String marketMakerId, long uid, boolean enabled) {
        return new MarketMakerProfile(
                marketMakerId,
                uid,
                enabled,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("1000000"),
                        new BigDecimal("1000000"),
                        new BigDecimal("10000"),
                        new BigDecimal("0.01"),
                        false
                ))
        );
    }

    private static class MemMarketMakerProfileStore implements MarketMakerProfileStore {
        private final Map<String, MarketMakerProfile> profiles = new LinkedHashMap<>();

        @Override
        public void save(MarketMakerProfile profile) {
            profiles.put(profile.marketMakerId(), profile);
        }

        @Override
        public Optional<MarketMakerProfile> findByMarketMakerId(String marketMakerId) {
            return Optional.ofNullable(profiles.get(marketMakerId));
        }

        @Override
        public Optional<MarketMakerProfile> findByUid(long uid) {
            return profiles.values().stream()
                    .filter(profile -> profile.uid() == uid)
                    .findFirst();
        }

        @Override
        public List<MarketMakerProfile> findEnabled() {
            return profiles.values().stream()
                    .filter(MarketMakerProfile::enabled)
                    .toList();
        }
    }
}
