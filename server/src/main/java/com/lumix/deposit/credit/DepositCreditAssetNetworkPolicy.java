package com.lumix.deposit.credit;

import com.lumix.account.AssetSymbol;
import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.finality.RequiredConfirmations;
import java.util.Objects;

/**
 * 指定 asset/network 組合的 immutable credit eligibility policy。
 *
 * <p>enabled 僅控制未來 handoff 是否可行，不會執行 credit；確認數仍必須由 P22 evidence independently 證明。</p>
 */
public record DepositCreditAssetNetworkPolicy(
        DepositCreditPolicyVersion version,
        DepositNetwork network,
        AssetSymbol asset,
        RequiredConfirmations requiredConfirmations,
        boolean enabled
) {

    public DepositCreditAssetNetworkPolicy {
        version = Objects.requireNonNull(version, "version must not be null");
        network = Objects.requireNonNull(network, "network must not be null");
        asset = Objects.requireNonNull(asset, "asset must not be null");
        requiredConfirmations = Objects.requireNonNull(requiredConfirmations, "requiredConfirmations must not be null");
    }
}
