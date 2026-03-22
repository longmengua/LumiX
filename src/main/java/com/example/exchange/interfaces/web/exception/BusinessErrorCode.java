package com.example.exchange.interfaces.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum BusinessErrorCode {

    AUTH_INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "登入已過期"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "不合法憑證"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "帳號已停權"),
    USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "帳號已註冊"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "參數錯誤"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系統錯誤"),
    ;

    private final HttpStatus status;
    private final String message;
}