package com.lumix.api.version;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 版本化 API endpoint 標記。
 *
 * 這個 marker 只提示後續 controller 要掛在版本化 path 下，不代表已經有真實 controller。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ApiVersionedEndpoint {

    /**
     * API 版本字串。
     *
     * 預設採 `/api/v1` 對應的 `v1`，讓未來版本升級時有明確替換點。
     */
    String value() default "v1";
}
