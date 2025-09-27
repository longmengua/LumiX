package com.example.exchange.interfaces.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 自訂校驗註解：交易對格式校驗（簡化版）
 * - 預期格式：<BASE><QUOTE>，例如 BTCUSDT、ETHUSDT
 * - 你也可改為查表/查快取（允許的 symbol 列表）
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSymbolValidator.class)
public @interface ValidSymbol {
    String message() default "invalid symbol format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
