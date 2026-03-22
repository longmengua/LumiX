package com.example.exchange.interfaces.web.exception;

public record ErrorResponse(
        int code,
        String message,
        String traceId
) {}
