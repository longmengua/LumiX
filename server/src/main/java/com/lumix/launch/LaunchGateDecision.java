package com.lumix.launch;
/** P36 aggregate outcome；任何非 NOT_READY 都不是 production launch 授權。 */
public enum LaunchGateDecision { NOT_READY_EVIDENCE_MISSING, NOT_READY_HUMAN_SIGN_OFF_MISSING, READY_FOR_HUMAN_SIGN_OFF_ONLY }
