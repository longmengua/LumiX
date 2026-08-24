package com.lumix.deposit.observation.reconciliation;

import com.lumix.deposit.observation.DepositChainObservation;
import com.lumix.deposit.observation.finality.DepositObservationFinalityState;
import java.util.Objects;

/**
 * 交給 P23 設計階段審閱的 finality evidence pair。
 *
 * <p>名稱雖為 handoff candidate，卻不是入帳命令；未來 credit 邊界仍需獨立的所有權、帳本與人工安全審核。</p>
 */
public record P23DepositHandoffCandidate(
        DepositChainObservation observation,
        DepositObservationFinalityState finalityState
) {

    public P23DepositHandoffCandidate {
        observation = Objects.requireNonNull(observation, "observation must not be null");
        finalityState = Objects.requireNonNull(finalityState, "finalityState must not be null");
        if (!observation.identity().equals(finalityState.identity()) || !observation.block().equals(finalityState.block())) {
            throw new IllegalArgumentException("handoff evidence identity and block must agree");
        }
    }
}
