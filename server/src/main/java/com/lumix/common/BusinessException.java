package com.lumix.common;

import java.util.Objects;

/**
 * 業務層例外。
 * 讓驗證失敗與系統錯誤分離，方便之後接入 API 錯誤映射。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        // 錯誤碼是外層處理的重要依據，不能為空。
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * 取得可供 API / controller 映射的業務錯誤碼。
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
