package com.lumix.user.auth.api;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.api.error.ApiException;
import com.lumix.api.error.ApiExceptionHandler;
import com.lumix.common.RequestId;
import java.util.UUID;
import java.sql.SQLException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 認證 endpoint 的去敏錯誤 adapter，禁止將 credential、token、SQL 或 stack trace 帶回瀏覽器。 */
@RestControllerAdvice(assignableTypes = UserAuthenticationController.class)
public class UserAuthenticationExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthenticationExceptionHandler.class);
    private final ApiExceptionHandler errorHandler = new ApiExceptionHandler();

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, WebRequest request) {
        return response(errorHandler.toResponse(exception, requestId(request)), exception.getErrorCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception, WebRequest request) {
        return response(ApiErrorResponse.of(ApiErrorCode.VALIDATION_ERROR, requestId(request), null), ApiErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, WebRequest request) {
        RequestId requestId = requestId(request);
        // 只記錄例外型別與 requestId，避免 password、token、email、SQL 或 stack trace 進入集中式日誌。
        SQLException sqlException = exception.getCause() instanceof SQLException candidate ? candidate : null;
        LOGGER.error("Authentication request failed: requestId={}, exceptionType={}, causeType={}",
            requestId.value(), exception.getClass().getName(),
            exception.getCause() == null ? null : exception.getCause().getClass().getName());
        if (sqlException != null) {
            // SQL state/code 為 PostgreSQL 標準分類，不包含 query 或 bind parameter，可用於安全診斷。
            LOGGER.error("Authentication database failure: requestId={}, sqlState={}, vendorCode={}",
                requestId.value(), sqlException.getSQLState(), sqlException.getErrorCode());
        }
        return response(errorHandler.toResponse(exception, requestId), ApiErrorCode.INTERNAL_ERROR);
    }

    private static ResponseEntity<ApiErrorResponse> response(ApiErrorResponse body, ApiErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    private static RequestId requestId(WebRequest request) {
        String supplied = request.getHeader("X-Request-Id");
        // 不信任任意長的外部 header，避免把攻擊者資料複製到所有 error response。
        return supplied != null && !supplied.isBlank() && supplied.length() <= 128
            ? new RequestId(supplied)
            : new RequestId(UUID.randomUUID().toString());
    }
}
