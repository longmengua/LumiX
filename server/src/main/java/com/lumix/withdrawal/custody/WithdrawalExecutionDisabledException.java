package com.lumix.withdrawal.custody;

/** deployment 未明確開啟提款時的 fail-closed 拒絕；呼叫端不得降級為本機簽章。 */
public final class WithdrawalExecutionDisabledException extends IllegalStateException {
    public WithdrawalExecutionDisabledException() { super("withdrawal execution is disabled"); }
}
