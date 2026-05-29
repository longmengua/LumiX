/*
 * 檔案用途：JPA adapter，實作體驗金 grant read model store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.BonusCreditGrant;
import com.example.exchange.domain.model.entity.BonusCreditGrantRecord;
import com.example.exchange.domain.repository.BonusCreditGrantStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaBonusCreditGrantStore implements BonusCreditGrantStore {

    private final BonusCreditGrantRecordJpaRepository repository;

    @Override
    @Transactional
    public void save(BonusCreditGrant grant) {
        repository.save(BonusCreditGrantRecord.from(grant, BonusCreditGrantStore.SCHEMA_VERSION));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BonusCreditGrant> findActiveByUidAndAsset(long uid, String asset) {
        return repository.findActiveForConsumption(
                        uid,
                        asset,
                        BonusCreditGrant.ACTIVE
                )
                .stream()
                .map(BonusCreditGrantRecord::toGrant)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BonusCreditGrant> findActiveExpiringAtOrBefore(Instant now, int limit) {
        int normalizedLimit = Math.min(Math.max(1, limit), 1000);
        return repository.findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAscIdAsc(
                        BonusCreditGrant.ACTIVE,
                        now,
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(BonusCreditGrantRecord::toGrant)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BonusCreditGrant> findByUid(long uid) {
        return repository.findByUidOrderByGrantedAtAscIdAsc(uid)
                .stream()
                .map(BonusCreditGrantRecord::toGrant)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BonusCreditGrant> findByCampaignId(String campaignId) {
        if (campaignId == null || campaignId.isBlank()) return List.of();
        return repository.findByCampaignIdOrderByGrantedAtAscIdAsc(campaignId.trim())
                .stream()
                .map(BonusCreditGrantRecord::toGrant)
                .toList();
    }
}
