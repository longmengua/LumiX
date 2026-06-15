/*
 * 檔案用途：Web 例外處理，統一業務錯誤碼與 HTTP 回應格式。
 */
package com.example.exchange.interfaces.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum BusinessErrorCode {

    AUTH_INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤"),
    AUTH_REGISTRATION_PENDING(HttpStatus.CONFLICT, "註冊驗證進行中"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "登入已過期"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "不合法憑證"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "帳號已停權"),
    USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "帳號已註冊"),
    ORDER_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "可用餘額不足"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "參數錯誤"),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "分類不合法"),
    INVALID_CHANNEL(HttpStatus.BAD_REQUEST, "通道參數不合法"),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "分頁游標不合法"),
    INVALID_AUDIENCE(HttpStatus.BAD_REQUEST, "受眾條件不合法"),
    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "游標格式不合法"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系統錯誤"),

    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "訊息不存在"),
    MESSAGE_NOT_OWNED(HttpStatus.FORBIDDEN, "沒有權限存取這則訊息"),
    PREFERENCE_LOCKED(HttpStatus.CONFLICT, "該訊息分類偏好為不可變更"),
    SCHEDULE_NOT_CANCELABLE(HttpStatus.CONFLICT, "公告無法取消"),
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "公告不存在"),
    DUPLICATE_MESSAGE(HttpStatus.OK, "訊息已存在，處理為冪等成功")
    ;

    private final HttpStatus status;
    private final String message;
}
