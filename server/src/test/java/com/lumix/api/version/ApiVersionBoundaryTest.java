package com.lumix.api.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 驗證 API version path convention 與文件 boundary。
 *
 * 這組測試保護的是對外契約，不是 controller 實作。
 */
class ApiVersionBoundaryTest {

    /**
     * 確認 versioned path 一律掛在 `/api/v1` 底下。
     */
    @Test
    void versionedPathStartsWithApiV1() {
        assertEquals("/api/v1/users", ApiVersionPaths.v1("users"));
        assertEquals("/api/v1/orders/123", ApiVersionPaths.v1("/orders/123"));
    }

    /**
     * 確認只有 `/api/v1` 才算正式版本化 API path。
     */
    @Test
    void versionedPathCheckRejectsUnversionedPaths() {
        assertTrue(ApiVersionPaths.isVersionedV1Path("/api/v1/users"));
        assertTrue(ApiVersionPaths.isVersionedV1Path("/api/v1"));
        assertFalse(ApiVersionPaths.isVersionedV1Path("/users"));
        assertFalse(ApiVersionPaths.isVersionedV1Path("/api/v2/users"));
    }
}
