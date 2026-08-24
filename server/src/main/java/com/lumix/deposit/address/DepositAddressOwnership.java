package com.lumix.deposit.address;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import java.util.Objects;

/**
 * 一筆 immutable 入金地址所有權聲明。
 *
 * <p>address generation、wallet provisioning 與 credit 均不在這個模型內；它只固定後續 chain observation
 * 能追查的 owner/network/asset/address identity。</p>
 */
public record DepositAddressOwnership(
        UserId ownerUserId,
        AssetSymbol asset,
        DepositNetwork network,
        DepositAddress address,
        DepositAddressLifecycle lifecycle
) {

    public DepositAddressOwnership {
        ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
        asset = Objects.requireNonNull(asset, "asset must not be null");
        network = Objects.requireNonNull(network, "network must not be null");
        address = Objects.requireNonNull(address, "address must not be null");
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }

    /** network/address 是跨 asset 的物理地址 identity，避免同一地址被不同使用者聲明。 */
    public NetworkAddressKey networkAddressKey() {
        return new NetworkAddressKey(network, address);
    }

    public record NetworkAddressKey(DepositNetwork network, DepositAddress address) {
        public NetworkAddressKey {
            network = Objects.requireNonNull(network, "network must not be null");
            address = Objects.requireNonNull(address, "address must not be null");
        }
    }
}
