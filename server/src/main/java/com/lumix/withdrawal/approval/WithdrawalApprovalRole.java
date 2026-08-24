package com.lumix.withdrawal.approval;

/** 將審核職責拆成互斥證據，避免單一角色或單一人員自行完成提款核准。 */
public enum WithdrawalApprovalRole {
    REVIEWER,
    SENIOR_REVIEWER
}
