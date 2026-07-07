package com.lumix.security;

/**
 * security risk level。
 *
 * 用來標記高風險操作是否需要 HUMAN_REVIEW_REQUIRED 或 production-gated。
 */
public enum SecurityRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    PRODUCTION_GATED
}
