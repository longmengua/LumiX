package com.example.exchange.interfaces.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler
 *
 * 全域例外處理器（Infra 層）
 *
 * 作用：
 * 1. 統一所有 API 錯誤輸出格式
 * 2. 將 Exception 轉換為標準 HTTP Response
 * 3. 避免 Controller 內出現 try-catch
 *
 * 設計原則：
 * - Service 只丟 BusinessException
 * - Controller 不處理錯誤
 * - 所有錯誤由這裡統一轉換
 *
 * 好處：
 * - API 格式一致
 * - HTTP Status 正確
 * - 錯誤碼可控
 * - 可搭配 traceId 做問題追蹤
 *
 * 整個資料留：
 * Domain / Service
 *         ↓ throw
 * BusinessException
 *         ↓
 * GlobalExceptionHandler（Infra）
 *         ↓
 * HTTP Response
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 處理業務例外（BusinessException）
     *
     * 這類例外通常來自：
     * - 登入錯誤
     * - 權限不足
     * - 參數驗證錯誤
     * - 業務規則違反
     *
     * 流程：
     * BusinessException
     *      ↓
     * 取得 BizErrorCode
     *      ↓
     * 轉成 HTTP Status + ErrorResponse
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {

        // 取得對應的錯誤碼列舉
        BusinessErrorCode code = ex.getErrorCode();

        return ResponseEntity
                // HTTP status 由錯誤碼決定（協議層語意）
                .status(code.getStatus())
                .body(new ErrorResponse(
                        // 業務錯誤代碼（前端判斷依據）
                        code.getStatus().value(),

                        // 對外顯示訊息（避免直接回傳 ex.getMessage()）
                        code.getMessage(),

                        // traceId 用於串接 log（需搭配 MDC 設定）
                        MDC.get("traceId")
                ));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        400,
                        "參數或業務條件不合法",
                        MDC.get("traceId")
                ));
    }

    /**
     * 處理未預期例外（系統錯誤）
     *
     * 包含：
     * - NullPointerException
     * - DB 連線錯誤
     * - 第三方服務錯誤
     * - 任何未被捕捉的 RuntimeException
     *
     * 設計原則：
     * - 不將詳細錯誤暴露給前端
     * - 僅回傳統一 INTERNAL_ERROR
     * - 詳細錯誤應記錄於 log（搭配 traceId）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {

        // 不要把 ex.getMessage() 回傳給前端
        log.error("Unexpected error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        500,
                        "系統發生錯誤",
                        MDC.get("traceId")
                ));
    }
}
