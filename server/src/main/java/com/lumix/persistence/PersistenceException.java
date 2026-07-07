package com.lumix.persistence;

import java.util.Objects;

/**
 * persistence boundary 的受控例外。
 *
 * 這個例外只攜帶受控的 persistence error code，避免 raw SQLException 或 SQL 字串直接往上拋。
 */
public class PersistenceException extends RuntimeException {

    private final PersistenceErrorCode errorCode;

    /**
     * 建立 persistence exception。
     *
     * message 會固定使用安全的預設敘述，不接受 raw SQL 當作公開訊息。
     */
    public PersistenceException(PersistenceErrorCode errorCode, Throwable cause) {
        super(resolveMessage(errorCode), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * 取得 persistence error code。
     */
    public PersistenceErrorCode getErrorCode() {
        return errorCode;
    }

    private static String resolveMessage(PersistenceErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return switch (errorCode) {
            case CONSTRAINT_VIOLATION -> "資料已存在或狀態衝突";
            case NOT_FOUND -> "查無資料";
            case CONNECTION_FAILURE, QUERY_FAILURE, UNKNOWN -> "資料存取失敗";
        };
    }
}
