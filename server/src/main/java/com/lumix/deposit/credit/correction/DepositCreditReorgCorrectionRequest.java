package com.lumix.deposit.credit.correction;

import com.lumix.deposit.credit.DepositCreditDecisionRecord;
import com.lumix.deposit.observation.finality.DepositObservationFinalityState;
import java.util.Objects;
import java.util.Optional;

/**
 * 已被 reorg observer 標記的 evidence 與原 credit decision record。
 *
 * <p>reversal order 僅在原 credit 已 append 時才有意義；缺少或不可信 append evidence 必須升級人工。</p>
 */
public record DepositCreditReorgCorrectionRequest(
        DepositCreditDecisionRecord originalDecision,
        DepositObservationFinalityState reorgState,
        DepositCreditAppendState appendState,
        Optional<AppendOnlyCorrectionOrder> correctionOrder
) {

    public DepositCreditReorgCorrectionRequest {
        originalDecision = Objects.requireNonNull(originalDecision, "originalDecision must not be null");
        reorgState = Objects.requireNonNull(reorgState, "reorgState must not be null");
        appendState = Objects.requireNonNull(appendState, "appendState must not be null");
        correctionOrder = Objects.requireNonNull(correctionOrder, "correctionOrder must not be null");
    }
}
