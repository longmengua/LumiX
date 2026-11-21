package com.lumix.trading.runtime;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiErrorResponse;
import com.lumix.common.RequestId;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 交易查詢只回安全錯誤碼；SQL、游標內容與他人訂單資訊不得出現在 response。 */
@RestControllerAdvice(assignableTypes = {TradingOrderQueryController.class, SpotTradingCommandController.class, TradingWorkspaceQueryController.class, TradingPolicyAdministrationController.class, TradingConfigurationController.class})
public class TradingOrderQueryExceptionHandler {
    /** 查無、格式錯誤及非預期服務錯誤分別分類，response 不附帶內部 exception message。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(Exception exception) {
        ApiErrorCode code = exception instanceof com.lumix.api.error.ApiException api ? api.getErrorCode()
                : exception instanceof TradingOrderRejectedException ? ApiErrorCode.HIGH_RISK_OPERATION_REJECTED
                : exception instanceof NoSuchElementException ? ApiErrorCode.NOT_FOUND
                : exception instanceof IllegalArgumentException || exception instanceof MethodArgumentTypeMismatchException
                || exception instanceof com.fasterxml.jackson.core.JsonProcessingException
                || exception instanceof org.springframework.web.bind.ServletRequestBindingException
                ? ApiErrorCode.VALIDATION_ERROR : ApiErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getHttpStatus()).header("Cache-Control", "no-store")
                .body(ApiErrorResponse.of(code, new RequestId(UUID.randomUUID().toString()), null));
    }
}
