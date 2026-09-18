package com.lumix.api.error;

import com.lumix.common.ErrorCode;

/**
 * 對外錯誤碼集合。
 *
 * 這一層刻意把錯誤分類做得比內部例外更穩定，避免 API contract 跟著 runtime 細節漂移。
 */
public enum ApiErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "請求驗證失敗", 400, false),
    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", "認證失敗", 401, false),
    AUTHORIZATION_ERROR("AUTHORIZATION_ERROR", "權限不足", 403, false),
    TRANSPORT_SECURITY_REQUIRED("TRANSPORT_SECURITY_REQUIRED", "必須使用安全傳輸連線", 400, false),
    NOT_FOUND("NOT_FOUND", "查無資料", 404, false),
    CONFLICT("CONFLICT", "資源衝突", 409, false),
    EMAIL_ALREADY_REGISTERED("EMAIL_ALREADY_REGISTERED", "此電子郵件已被註冊", 409, true),
    CURRENT_PASSWORD_INCORRECT("CURRENT_PASSWORD_INCORRECT", "目前密碼錯誤", 400, false),
    NEW_PASSWORD_SAME_AS_CURRENT("NEW_PASSWORD_SAME_AS_CURRENT", "新密碼不得與目前密碼相同", 400, false),
    CAPTCHA_REQUIRED("CAPTCHA_REQUIRED", "請先完成滑動驗證", 400, false),
    CAPTCHA_INVALID("CAPTCHA_INVALID", "滑動驗證已失效，請重新完成驗證", 400, false),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "服務暫時無法使用", 503, false),
    RATE_LIMITED("RATE_LIMITED", "請求過於頻繁", 429, false),
    HIGH_RISK_OPERATION_REJECTED("HIGH_RISK_OPERATION_REJECTED", "高風險操作已被拒絕", 422, true),
    INVALID_ADJUSTMENT_AMOUNT("INVALID_ADJUSTMENT_AMOUNT", "資產調整金額無效", 400, false),
    INVALID_BUSINESS_REFERENCE("INVALID_BUSINESS_REFERENCE", "業務來源參考無效", 400, false),
    INSUFFICIENT_USER_BALANCE("INSUFFICIENT_USER_BALANCE", "使用者可用餘額不足", 422, true),
    SOURCE_LEDGER_ENTRY_NOT_FOUND("SOURCE_LEDGER_ENTRY_NOT_FOUND", "找不到來源帳本分錄", 404, false),
    SOURCE_LEDGER_ENTRY_NOT_ELIGIBLE("SOURCE_LEDGER_ENTRY_NOT_ELIGIBLE", "來源帳本分錄不可沖銷", 422, true),
    REVERSAL_AMOUNT_EXCEEDS_REMAINING("REVERSAL_AMOUNT_EXCEEDS_REMAINING", "沖銷數量超過剩餘可沖回額度", 422, true),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", "冪等鍵已用於不同請求", 409, false),
    INVALID_ACCOUNT_PURPOSE("INVALID_ACCOUNT_PURPOSE", "系統帳戶用途不符合此命令", 422, true),
    ADJUSTMENT_NOT_FOUND("ADJUSTMENT_NOT_FOUND", "找不到資產調整紀錄", 404, false),
    INTERNAL_ERROR("INTERNAL_ERROR", "系統發生錯誤", 500, false);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;
    private final boolean humanReviewRequired;

    ApiErrorCode(String code, String defaultMessage, int httpStatus, boolean humanReviewRequired) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
        this.humanReviewRequired = humanReviewRequired;
    }

    /**
     * 取得對外穩定錯誤碼字串。
     *
     * API contract 只應依賴這個字串，不應依賴 enum 名稱本身。
     */
    public String getCode() {
        return code;
    }

    /**
     * 取得預設公開訊息。
     *
     * handler 會優先使用這個值，避免把內部 exception message 直接外洩。
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * 取得預期 HTTP status。
     *
     * 這裡先保留數值，讓 boundary 不必先依賴 web package 也能被測試。
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 標示這類錯誤是否需要保留 HUMAN_REVIEW_REQUIRED 語意。
     *
     * ledger、withdrawal、settlement 與 risk 類拒絕都不應被淡化成一般 client error。
     */
    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }

    /**
     * 將既有內部錯誤碼保守映射到 API 錯誤碼。
     *
     * 這裡故意偏向保守分類，避免把內部帳務或流程細節直接暴露成更具體的公開錯誤。
     */
    public static ApiErrorCode fromCommonErrorCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return INTERNAL_ERROR;
        }

        return switch (errorCode) {
            case INVALID_REQUEST, INVALID_AMOUNT, UNSUPPORTED_ACCOUNT_TYPE -> VALIDATION_ERROR;
            case SAME_ACCOUNT_TRANSFER_NOT_ALLOWED -> CONFLICT;
            case NOT_IMPLEMENTED -> INTERNAL_ERROR;
        };
    }
}
