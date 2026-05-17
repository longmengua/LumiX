/*
 * 檔案用途：Web 例外處理，統一業務錯誤碼與 HTTP 回應格式。
 */
package com.example.exchange.interfaces.web.exception;

/**
 * 範例商業例外
 * - 你可在 domain/application 使用它來拋出可預期錯誤
 */
public class BusinessException extends RuntimeException {

    private final BusinessErrorCode businessErrorCode;

    public BusinessException(BusinessErrorCode businessErrorCode) {
        super(businessErrorCode.getMessage());
        this.businessErrorCode = businessErrorCode;
    }

    public BusinessErrorCode getErrorCode() {
        return businessErrorCode;
    }
}