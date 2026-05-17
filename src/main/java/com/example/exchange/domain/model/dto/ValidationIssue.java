package com.example.exchange.domain.model.dto;

public record ValidationIssue(
        String severity,
        String code,
        String message
) {}
