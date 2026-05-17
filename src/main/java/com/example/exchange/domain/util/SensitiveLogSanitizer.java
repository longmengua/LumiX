/*
 * 檔案用途：日誌安全工具，統一遮罩 private key、API secret、簽名與授權資訊。
 */
package com.example.exchange.domain.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitizes log messages before they are written to application logs.
 */
public final class SensitiveLogSanitizer {

    private static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization",
            "proxyauthorization",
            "cookie",
            "setcookie",
            "apikey",
            "xapikey",
            "apisecret",
            "apipassphrase",
            "passphrase",
            "privatekey",
            "signature",
            "polysignature",
            "polyapikey",
            "polypassphrase"
    );

    private static final Pattern AUTHORIZATION_PATTERN =
            Pattern.compile("(?i)((?:\"|')?(?:authorization|proxy[-_ ]?authorization)(?:\"|')?\\s*[:=]\\s*)([\"']?)(?:Bearer\\s+)?([^\"',;&}\\]]+)([\"']?)");

    private static final Pattern SENSITIVE_ASSIGNMENT_PATTERN =
            Pattern.compile("(?i)((?:\"|')?(?:cookie|set[-_ ]?cookie|x[-_ ]?api[-_ ]?key|api[-_ ]?key|api[-_ ]?secret|api[-_ ]?passphrase|passphrase|private[-_ ]?key|poly[-_ ]?signature|poly[-_ ]?api[-_ ]?key|poly[-_ ]?passphrase|signature)(?:\"|')?\\s*[:=]\\s*)([\"']?)([^\"'\\s,;&}\\]]+)([\"']?)");

    private SensitiveLogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String sanitized = maskPattern(value, AUTHORIZATION_PATTERN);
        return maskPattern(sanitized, SENSITIVE_ASSIGNMENT_PATTERN);
    }

    public static String sanitizeHeader(String headerName, String headerValue) {
        if (isSensitiveKey(headerName)) {
            return MASK;
        }
        return sanitize(headerValue);
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return SENSITIVE_KEYS.contains(normalizeKey(key));
    }

    private static String maskPattern(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer sanitized = new StringBuffer();

        while (matcher.find()) {
            String replacement =
                    Matcher.quoteReplacement(
                            matcher.group(1)
                                    + matcher.group(2)
                                    + MASK
                                    + matcher.group(4)
                    );
            matcher.appendReplacement(sanitized, replacement);
        }

        matcher.appendTail(sanitized);
        return sanitized.toString();
    }

    private static String normalizeKey(String key) {
        return key
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}
