package com.lumix.application.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * application service transaction boundary 標記。
 *
 * 這個 marker 只用來提醒後續實作者：transaction 應該在 application service 層定義，
 * 而不是讓 controller 或 repository 各自決定。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ApplicationTransactionBoundary {
}
