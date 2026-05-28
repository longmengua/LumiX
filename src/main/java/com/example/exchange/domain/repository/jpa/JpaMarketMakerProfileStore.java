/*
 * 檔案用途：JPA adapter，實作做市商 profile 與 risk limit store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.entity.MarketMakerProfileRecord;
import com.example.exchange.domain.model.entity.MarketMakerRiskLimitRecord;
import com.example.exchange.domain.repository.MarketMakerProfileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMarketMakerProfileStore implements MarketMakerProfileStore {

    private final MarketMakerProfileRecordJpaRepository profileRepository;
    private final MarketMakerRiskLimitRecordJpaRepository riskLimitRepository;

    @Override
    @Transactional
    public void save(MarketMakerProfile profile) {
        profileRepository.save(MarketMakerProfileRecord.from(profile, MarketMakerProfileStore.SCHEMA_VERSION));
        riskLimitRepository.deleteByMarketMakerId(profile.marketMakerId());
        riskLimitRepository.saveAll(profile.riskLimits().stream()
                .map(limit -> MarketMakerRiskLimitRecord.from(profile.marketMakerId(), limit))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketMakerProfile> findByMarketMakerId(String marketMakerId) {
        if (marketMakerId == null || marketMakerId.isBlank()) return Optional.empty();
        return profileRepository.findById(marketMakerId.trim())
                .map(this::toProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketMakerProfile> findByUid(long uid) {
        return profileRepository.findByUid(uid)
                .map(this::toProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketMakerProfile> findEnabled() {
        return profileRepository.findByEnabledTrueOrderByMarketMakerIdAsc()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    private MarketMakerProfile toProfile(MarketMakerProfileRecord record) {
        List<MarketMakerRiskLimit> limits = riskLimitRepository
                .findByMarketMakerIdOrderBySymbolAsc(record.getMarketMakerId())
                .stream()
                .map(MarketMakerRiskLimitRecord::toLimit)
                .toList();
        return new MarketMakerProfile(
                record.getMarketMakerId(),
                record.getUid(),
                record.getEnabled(),
                limits
        );
    }
}
