package com.lumix.wallet.provider.mock;

import com.lumix.wallet.ChainType;
import com.lumix.wallet.provider.ChainProvider;
import com.lumix.wallet.provider.ChainProviderCapability;
import com.lumix.wallet.provider.ChainProviderHealth;
import com.lumix.wallet.provider.ChainProviderObservation;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 僅供 `chain-mock` profile 的鏈上 observation adapter。
 *
 * <p>這是人類明確允許的唯一 mock 邊界；它只提供明確註冊的 chain observation，不產生地址、
 * 不持有金鑰、不呼叫 ledger credit，也不會在任何 production profile 被建立。</p>
 */
@Component
@Profile("chain-mock")
public class MockChainProvider implements ChainProvider {
    private final List<ChainProviderObservation> observations = new CopyOnWriteArrayList<>();

    @Override public String providerId() { return "MOCK_CHAIN"; }
    @Override public Set<ChainType> supportedChains() { return Set.of(ChainType.values()); }
    @Override public Set<ChainProviderCapability> capabilities() { return Set.of(ChainProviderCapability.DEPOSIT_OBSERVATION); }
    @Override public ChainProviderHealth health() { return new ChainProviderHealth(true, Instant.now(), "chain-mock-profile-only"); }

    /** 僅供隔離測試／開發 fixture 註冊 observation；production path 不提供此操作。 */
    public void registerObservation(ChainProviderObservation observation) { observations.add(observation); }

    @Override
    public List<ChainProviderObservation> observeAfter(String cursor, Instant observedBefore) {
        return observations.stream().filter(value -> value.observedAt().isBefore(observedBefore)
                        || value.observedAt().equals(observedBefore))
                .filter(value -> cursor == null || value.cursor().compareTo(cursor) > 0).toList();
    }
}
