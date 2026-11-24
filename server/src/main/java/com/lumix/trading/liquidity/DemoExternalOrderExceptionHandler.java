package com.lumix.trading.liquidity;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.common.RequestId;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Demo API 仍使用去敏錯誤格式，避免開啟測試開關後洩漏內部 exception。 */
@RestControllerAdvice(assignableTypes = {DemoExternalOrderController.class,
        ExternalOrderProxySettingsController.class})
public class DemoExternalOrderExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(Exception exception) {
        ApiErrorCode code = exception instanceof com.lumix.api.error.ApiException api ? api.getErrorCode()
                : exception instanceof java.util.NoSuchElementException ? ApiErrorCode.NOT_FOUND
                : exception instanceof IllegalStateException ? ApiErrorCode.CONFLICT
                : exception instanceof IllegalArgumentException
                || exception instanceof org.springframework.http.converter.HttpMessageNotReadableException
                ? ApiErrorCode.VALIDATION_ERROR : ApiErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getHttpStatus()).header("Cache-Control", "no-store")
                .body(ApiErrorResponse.of(code, new RequestId(UUID.randomUUID().toString()), null));
    }
}
