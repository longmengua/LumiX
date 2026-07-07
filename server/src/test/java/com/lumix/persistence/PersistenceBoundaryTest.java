package com.lumix.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.api.error.ApiExceptionHandler;
import com.lumix.common.RequestId;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * 驗證 persistence boundary 不會把 SQL 或內部例外直接外洩到 API 層。
 */
class PersistenceBoundaryTest {

    /**
     * 確認 constraint violation 會被保守映射成 CONFLICT。
     *
     * 這種情境常見於唯一鍵或狀態衝突，應保留安全分類但不能暴露 raw SQL。
     */
    @Test
    void constraintViolationMapsToConflictWithoutSqlLeak() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        PersistenceException exception = new PersistenceException(
            PersistenceErrorCode.CONSTRAINT_VIOLATION,
            new SQLException("insert into ledger values ('secret-sql')")
        );

        ApiErrorResponse response = handler.toResponse(exception, new RequestId("req-901"));

        assertEquals(ApiErrorCode.CONFLICT.getCode(), response.code());
        assertEquals(ApiErrorCode.CONFLICT.getDefaultMessage(), response.message());
        assertEquals("req-901", response.requestId());
        assertNull(response.details());
        assertFalse(response.message().contains("insert"));
    }

    /**
     * 確認資料庫連線或查詢失敗不會把內部訊息直接送回 API。
     */
    @Test
    void queryFailureMapsToInternalErrorWithoutDetailsLeak() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        PersistenceException exception = new PersistenceException(
            PersistenceErrorCode.QUERY_FAILURE,
            new SQLException("connection string should not leak")
        );

        ApiErrorResponse response = handler.toResponse(exception, new RequestId("req-902"));

        assertEquals(ApiErrorCode.INTERNAL_ERROR.getCode(), response.code());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.getDefaultMessage(), response.message());
        assertEquals("req-902", response.requestId());
        assertNull(response.details());
        assertFalse(response.message().contains("connection string"));
    }
}
