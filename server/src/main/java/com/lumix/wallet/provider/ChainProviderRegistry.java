package com.lumix.wallet.provider;

import com.lumix.wallet.ChainType;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 多 provider adapter 的選擇邊界。
 *
 * <p>registry 不建立 fallback fixture，也不跨 provider 拼湊 observation。若沒有宣告支援且 health
 * 可用的 adapter，呼叫端必須顯示 unavailable 並停止資金流程。</p>
 */
@Component
public class ChainProviderRegistry {
    private final List<ChainProvider> providers;

    public ChainProviderRegistry(List<ChainProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
    }

    /** 尋找同時支援指定 chain 與 capability 的健康 adapter；providerId 排序確保選擇結果 deterministic。 */
    public Optional<ChainProvider> findAvailable(ChainType chainType, ChainProviderCapability capability) {
        Objects.requireNonNull(chainType, "chainType must not be null");
        Objects.requireNonNull(capability, "capability must not be null");
        return providers.stream()
                .filter(provider -> provider.supportedChains().contains(chainType))
                .filter(provider -> provider.capabilities().contains(capability))
                .sorted(Comparator.comparing(ChainProvider::providerId))
                .filter(provider -> provider.health().available())
                .findFirst();
    }
}
