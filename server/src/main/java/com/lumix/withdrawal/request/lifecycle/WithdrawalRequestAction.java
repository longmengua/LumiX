package com.lumix.withdrawal.request.lifecycle;

/** P24-T03 中不具資金副作用的 request transition 意圖。 */
public enum WithdrawalRequestAction { CANCEL, EXPIRE, QUEUE_MANUAL_REVIEW, PREPARE_APPROVAL_HANDOFF }
