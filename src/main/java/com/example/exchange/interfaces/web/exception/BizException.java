package com.example.exchange.interfaces.web.exception;

/**
 * 範例商業例外
 * - 你可在 domain/application 使用它來拋出可預期錯誤
 */
public class BizException extends RuntimeException {
    public BizException(String message) { super(message); }
}
