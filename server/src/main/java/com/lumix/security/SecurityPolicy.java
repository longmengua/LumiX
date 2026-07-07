package com.lumix.security;

/**
 * security policy skeleton。
 *
 * 這個 policy 只做分類，不做身份驗證或授權執行，也不處理 secret。
 */
public class SecurityPolicy {

    /**
     * 將操作分類成風險等級。
     *
     * 高風險流程必須被標示，避免後續把 withdrawal / ledger / settlement 當成一般 API。
     */
    public SecurityRiskLevel classify(SecurityOperation operation) {
        if (operation == null) {
            return SecurityRiskLevel.HIGH;
        }

        return switch (operation) {
            case READ_ONLY_QUERY -> SecurityRiskLevel.LOW;
            case WITHDRAWAL_REQUEST, LEDGER_POSTING, ADMIN_ACTION, SETTLEMENT, RISK_OVERRIDE -> SecurityRiskLevel.PRODUCTION_GATED;
        };
    }

    /**
     * 判斷是否需要 HUMAN_REVIEW_REQUIRED。
     *
     * 這些操作屬於資金、管理權限或風控範圍，不能被簡化成普通授權判斷。
     */
    public boolean requiresHumanReview(SecurityOperation operation) {
        if (operation == null) {
            return true;
        }

        return switch (operation) {
            case WITHDRAWAL_REQUEST, LEDGER_POSTING, ADMIN_ACTION, SETTLEMENT, RISK_OVERRIDE -> true;
            case READ_ONLY_QUERY -> false;
        };
    }
}
