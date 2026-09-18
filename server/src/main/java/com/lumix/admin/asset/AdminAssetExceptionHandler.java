package com.lumix.admin.asset;

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

/** 管理端資產寫入失敗一律去敏，不能將 SQL、帳本帳戶或內部 idempotency 細節回傳瀏覽器。 */
@RestControllerAdvice(assignableTypes = {
        AdminAirdropController.class,
        AssetAdjustmentController.class,
        AdminAssetAdjustmentAuditController.class,
        AdminUserAssetQueryController.class
})
public class AdminAssetExceptionHandler {
    private final ApiExceptionHandler errorHandler = new ApiExceptionHandler();

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> unreadable(WebRequest request) {
        return response(ApiErrorResponse.of(ApiErrorCode.VALIDATION_ERROR, requestId(request), null), ApiErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> exception(Exception exception, WebRequest request) {
        ApiErrorResponse body = errorHandler.toResponse(exception, requestId(request));
        ApiErrorCode code = exception instanceof ApiException apiException ? apiException.getErrorCode()
                : exception instanceof IllegalArgumentException ? ApiErrorCode.VALIDATION_ERROR
                : exception instanceof IllegalStateException ? ApiErrorCode.CONFLICT : ApiErrorCode.INTERNAL_ERROR;
        return response(body, code);
    }

    private static ResponseEntity<ApiErrorResponse> response(ApiErrorResponse body, ApiErrorCode code) {
        return ResponseEntity.status(code.getHttpStatus()).header("Cache-Control", "no-store").body(body);
    }

    private static RequestId requestId(WebRequest request) {
        String supplied = request.getHeader("X-Request-Id");
        return supplied != null && !supplied.isBlank() && supplied.length() <= 64
                ? new RequestId(supplied) : new RequestId(UUID.randomUUID().toString());
    }
}
