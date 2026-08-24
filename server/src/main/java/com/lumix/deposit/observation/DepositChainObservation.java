package com.lumix.deposit.observation;

import com.lumix.account.AssetSymbol;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositNetwork;
import java.util.Objects;

/**
 * 單筆 provider-neutral 入金候選觀測。
 *
 * <p>這是鏈上事實的不可變快照，而非入帳指令；其中 finality 僅供後續風險判斷，不會在此層觸發 credit。</p>
 */
public record DepositChainObservation(
        DepositObservationIdentity identity,
        ChainBlockReference block,
        DepositAddress recipientAddress,
        AssetSymbol asset,
        DepositAtomicAmount amount,
        ObservationFinality finality
) {

    public DepositChainObservation {
        identity = Objects.requireNonNull(identity, "identity must not be null");
        block = Objects.requireNonNull(block, "block must not be null");
        recipientAddress = Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        asset = Objects.requireNonNull(asset, "asset must not be null");
        amount = Objects.requireNonNull(amount, "amount must not be null");
        finality = Objects.requireNonNull(finality, "finality must not be null");
        if (recipientAddress.format() != identity.network().addressFormat()) {
            throw new IllegalArgumentException("recipient address format must match observation network");
        }
    }

    public DepositNetwork network() {
        return identity.network();
    }

    /**
     * 以區塊高度、hash、交易與 event index 組成可重放游標；不可改用接收時間。
     */
    public DepositObservationCursor cursor() {
        return new DepositObservationCursor(network(), block, identity.transactionId(), identity.eventIndex());
    }
}
