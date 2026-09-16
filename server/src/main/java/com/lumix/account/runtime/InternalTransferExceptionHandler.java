package com.lumix.account.runtime;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.api.error.ApiException;
import com.lumix.api.error.ApiExceptionHandler;
import com.lumix.common.RequestId;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/** 劃轉拒絕與資料庫衝突一律回傳去敏錯誤，不將餘額、SQL 或 reservation 識別碼暴露給 browser。 */
@RestControllerAdvice(assignableTypes = InternalTransferController.class)
public class InternalTransferExceptionHandler {
    private final ApiExceptionHandler errors = new ApiExceptionHandler();
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> unreadable(WebRequest request) { return response(ApiErrorResponse.of(ApiErrorCode.VALIDATION_ERROR, requestId(request), null), ApiErrorCode.VALIDATION_ERROR); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> exception(Exception exception, WebRequest request) {
        ApiErrorCode code = exception instanceof ApiException api ? api.getErrorCode() : exception instanceof IllegalArgumentException ? ApiErrorCode.VALIDATION_ERROR : exception instanceof IllegalStateException ? ApiErrorCode.CONFLICT : ApiErrorCode.INTERNAL_ERROR;
        return response(errors.toResponse(exception, requestId(request)), code);
    }
    private static ResponseEntity<ApiErrorResponse> response(ApiErrorResponse body, ApiErrorCode code) { return ResponseEntity.status(code.getHttpStatus()).header("Cache-Control", "no-store").body(body); }
    private static RequestId requestId(WebRequest request) { String header = request.getHeader("X-Request-Id"); return header != null && !header.isBlank() && header.length() <= 64 ? new RequestId(header) : new RequestId(UUID.randomUUID().toString()); }
}
