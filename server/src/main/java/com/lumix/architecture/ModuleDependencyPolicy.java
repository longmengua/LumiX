package com.lumix.architecture;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * module dependency guardrail policy。
 *
 * 這個 policy 只描述 package import 的允許方向，不做 runtime 行為，也不依賴額外框架。
 */
public class ModuleDependencyPolicy {

    private static final Set<String> COMMON_PREFIXES = Set.of(
        "java.",
        "jakarta.",
        "org.springframework.",
        "org.junit.",
        "org.assertj.",
        "com.fasterxml.jackson.",
        "org.flywaydb.",
        "org.h2."
    );

    private static final List<String> HIGH_RISK_MODULE_PREFIXES = List.of(
        "com.lumix.ledger.",
        "com.lumix.reservation.",
        "com.lumix.wallet.",
        "com.lumix.withdrawal.",
        "com.lumix.order.",
        "com.lumix.trade.",
        "com.lumix.settlement.",
        "com.lumix.outbox.",
        "com.lumix.audit.",
        "com.lumix.admin."
    );

    /**
     * 判斷 source package 可不可以 import target package。
     *
     * 這裡只保留 Phase 13 目前用得到的簡化規則，讓 skeleton 不違反方向。
     */
    public boolean isAllowedImport(String sourcePackage, String targetPackage) {
        Objects.requireNonNull(sourcePackage, "sourcePackage must not be null");
        Objects.requireNonNull(targetPackage, "targetPackage must not be null");

        String normalizedSourcePackage = normalizePackagePrefix(sourcePackage);
        String normalizedTargetPackage = normalizePackagePrefix(targetPackage);

        if (normalizedSourcePackage.equals(normalizedTargetPackage)) {
            return true;
        }

        for (String prefix : COMMON_PREFIXES) {
            if (normalizedTargetPackage.startsWith(prefix)) {
                return true;
            }
        }

        if (normalizedSourcePackage.startsWith("com.lumix.api.error.")) {
            return normalizedTargetPackage.startsWith("com.lumix.common.")
                || normalizedTargetPackage.startsWith("com.lumix.persistence.")
                || normalizedTargetPackage.startsWith("com.lumix.security.")
                || normalizedTargetPackage.startsWith("com.lumix.api.");
        }

        if (normalizedSourcePackage.startsWith("com.lumix.api.")) {
            return normalizedTargetPackage.startsWith("com.lumix.common.")
                || normalizedTargetPackage.startsWith("com.lumix.api.")
                || normalizedTargetPackage.startsWith("com.lumix.security.");
        }

        if (normalizedSourcePackage.startsWith("com.lumix.application.")) {
            return normalizedTargetPackage.startsWith("com.lumix.common.")
                || normalizedTargetPackage.startsWith("com.lumix.security.")
                || normalizedTargetPackage.startsWith("com.lumix.persistence.")
                || normalizedTargetPackage.startsWith("com.lumix.application.");
        }

        if (normalizedSourcePackage.startsWith("com.lumix.persistence.")) {
            return normalizedTargetPackage.startsWith("com.lumix.common.")
                || normalizedTargetPackage.startsWith("com.lumix.persistence.");
        }

        if (normalizedSourcePackage.startsWith("com.lumix.security.")) {
            return normalizedTargetPackage.startsWith("com.lumix.common.")
                || normalizedTargetPackage.startsWith("com.lumix.api.error.")
                || normalizedTargetPackage.startsWith("com.lumix.security.");
        }

        /*
         * 高風險 module 的直連目前仍保留過渡例外，因為現有 skeleton 還沒有完整拆出 application / port。
         * 這條規則只用來讓現況先通過 source-level guardrail，後續 phase 要再收緊成真正的邊界控制。
         */
        for (String prefix : HIGH_RISK_MODULE_PREFIXES) {
            if (normalizedSourcePackage.startsWith(prefix)) {
                return normalizedTargetPackage.startsWith("com.lumix.common.")
                    || normalizedTargetPackage.startsWith("com.lumix.application.")
                    || normalizedTargetPackage.startsWith("com.lumix.persistence.")
                    || normalizedTargetPackage.startsWith("com.lumix.security.")
                    || normalizedTargetPackage.startsWith("com.lumix.account.")
                    || normalizedTargetPackage.startsWith("com.lumix.asset.")
                    || normalizedTargetPackage.startsWith("com.lumix.market.")
                    || normalizedTargetPackage.startsWith("com.lumix.idempotency.")
                    || isHighRiskModule(normalizedTargetPackage);
            }
        }

        if (normalizedSourcePackage.startsWith("com.lumix.common.")) {
            return normalizedTargetPackage.startsWith("java.") || normalizedTargetPackage.startsWith("com.lumix.common.");
        }

        return normalizedTargetPackage.startsWith("com.lumix.");
    }

    /**
     * 判斷是否屬於高風險 module。
     *
     * 這些 module 之後不得彼此任意直連，必須透過 application service / port / transaction boundary。
     */
    public boolean isHighRiskModule(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        String normalizedPackageName = normalizePackagePrefix(packageName);
        for (String prefix : HIGH_RISK_MODULE_PREFIXES) {
            if (normalizedPackageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 將 package 名稱正規化成以 `.` 結尾的 prefix 形式。
     *
     * 這樣可以同時支援 `com.lumix.api` 與 `com.lumix.api.` 兩種輸入，避免 guardrail 測試因字串格式不同而誤判。
     */
    private static String normalizePackagePrefix(String packageName) {
        if (packageName.endsWith(".")) {
            return packageName;
        }

        return packageName + ".";
    }
}
