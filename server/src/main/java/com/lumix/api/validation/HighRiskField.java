package com.lumix.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 高風險欄位標記。
 *
 * 這個 marker 用來提醒後續 API / DTO 實作者，這些欄位不能隨便做寬鬆驗證或直接外露 rejectedValue。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface HighRiskField {

    /**
     * 供維護用途的欄位描述。
     *
     * 不要求每次都填，但如果填了，應優先說明風險而不是重複欄位名稱。
     */
    String value() default "";
}
