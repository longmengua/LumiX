package com.lumix.api.version;

import java.util.Objects;

/**
 * API versioned path 的共用工具。
 *
 * 這個工具只負責拼出 `/api/v1` 路徑與基本檢查，不代表 controller 已經存在。
 */
public final class ApiVersionPaths {

    public static final String API_ROOT = "/api";
    public static final String V1_ROOT = "/api/v1";

    private ApiVersionPaths() {
        // 這是純工具類，不允許被實例化，避免誤以為它保存狀態。
    }

    /**
     * 將 relative path 組合成 versioned API path。
     *
     * relativePath 必須是相對路徑，例如 `users` 或 `orders/{id}`。
     */
    public static String v1(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        String normalized = relativePath.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }

        normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        return V1_ROOT + "/" + normalized;
    }

    /**
     * 判斷是否為正式 versioned API path。
     *
     * 這裡只接受 `/api/v1` 開頭的 path，避免未版本化 API 混入正式契約。
     */
    public static boolean isVersionedV1Path(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith(V1_ROOT + "/") || V1_ROOT.equals(path);
    }
}
