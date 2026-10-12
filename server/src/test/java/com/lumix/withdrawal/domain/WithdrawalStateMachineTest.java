package com.lumix.withdrawal.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** 已廣播提款不得倒退回 approval，否則會破壞鏈上與帳本的不可逆證據。 */
class WithdrawalStateMachineTest {
    private final WithdrawalStateMachine states=new WithdrawalStateMachine();
    @Test void permitsForwardBroadcastLifecycle(){assertDoesNotThrow(()->states.requireTransition(WithdrawalStatus.SIGNED,WithdrawalStatus.BROADCASTING));assertDoesNotThrow(()->states.requireTransition(WithdrawalStatus.BROADCASTING,WithdrawalStatus.BROADCASTED));}
    @Test void rejectsBroadcastedRollbackToApproved(){assertThrows(IllegalStateException.class,()->states.requireTransition(WithdrawalStatus.BROADCASTED,WithdrawalStatus.APPROVED));}
}
