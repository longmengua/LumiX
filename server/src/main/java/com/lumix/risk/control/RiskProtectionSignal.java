package com.lumix.risk.control;
/** 外部唯讀 protection signal；此 enum 不會凍結帳戶、改變限額或發送指令。 */
public enum RiskProtectionSignal { NONE, ACCOUNT_FROZEN, VELOCITY_EXCEEDED, WITHDRAWAL_COOLING_OFF, MANUAL_ESCALATION_REQUIRED }
