/*
 * 檔案用途：Web 安全工具，將 HTTP path 分類為交易、資金、管理或一般受保護 API。
 */
package com.example.exchange.interfaces.web.security;

public final class ProtectedApiClassifier {

    private ProtectedApiClassifier() {
    }

    public static ProtectedApiCategory classify(String path) {
        if (path == null) {
            return ProtectedApiCategory.UNKNOWN;
        }

        if (path.startsWith("/api/messages")
                || path.startsWith("/api/message-preferences")
                || path.startsWith("/api/system/messages")
                || path.startsWith("/api/admin/messages")) {
            return path.startsWith("/api/admin/") || path.startsWith("/api/system/")
                    ? ProtectedApiCategory.ADMIN
                    : ProtectedApiCategory.PROTECTED;
        }

        if (path.startsWith("/api/order")
                || path.startsWith("/api/prediction/orders")
                || path.startsWith("/api/prediction/session")) {
            return ProtectedApiCategory.TRADING;
        }

        if (path.startsWith("/api/margin")
                || path.startsWith("/api/prediction/approve")) {
            return ProtectedApiCategory.FUNDS;
        }

        if (path.startsWith("/api/risk")
                || path.startsWith("/api/admin")
                || path.startsWith("/api/market-maker")
                || path.startsWith("/api/recovery")
                || path.startsWith("/api/ops")
                || path.startsWith("/api/prediction/clob")
                || path.startsWith("/api/prediction/markets")
                || path.startsWith("/api/prediction/ws")) {
            return ProtectedApiCategory.ADMIN;
        }

        return ProtectedApiCategory.PROTECTED;
    }
}
