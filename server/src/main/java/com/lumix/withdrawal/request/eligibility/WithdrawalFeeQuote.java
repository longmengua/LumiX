package com.lumix.withdrawal.request.eligibility;

import com.lumix.account.AssetSymbol;
import com.lumix.withdrawal.request.WithdrawalNetwork;
import com.lumix.withdrawal.request.WithdrawalAtomicAmount;
import java.time.Instant;
import java.util.Objects;

/** immutable、帶版本與有效期限的提款 fee quote；不計費或扣費。 */
public record WithdrawalFeeQuote(String version, AssetSymbol asset, WithdrawalNetwork network, WithdrawalAtomicAmount fee, Instant expiresAt) {
    public WithdrawalFeeQuote {
        version = Objects.requireNonNull(version, "version must not be null").trim();
        asset = Objects.requireNonNull(asset, "asset must not be null");
        network = Objects.requireNonNull(network, "network must not be null");
        fee = Objects.requireNonNull(fee, "fee must not be null");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (version.isBlank()) {
            throw new IllegalArgumentException("fee quote version must not be blank");
        }
    }
}
