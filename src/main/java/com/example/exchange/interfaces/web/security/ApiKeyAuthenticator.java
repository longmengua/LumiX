/*
 * 檔案用途：Web 安全工具，驗證 API key 並解析角色與 scope。
 */
package com.example.exchange.interfaces.web.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiKeyAuthenticator {

    private final String apiKeys;

    public ApiKeyAuthenticator(String apiKeys) {
        this.apiKeys = apiKeys == null ? "" : apiKeys;
    }

    public Optional<ApiPrincipal> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKeys.isBlank()) {
            return Optional.empty();
        }

        byte[] actualHash = sha256(apiKey.trim());

        for (String entry : apiKeys.split(";")) {
            Optional<ApiKeyRecord> record = parse(entry);
            if (record.isEmpty()) {
                continue;
            }

            byte[] expectedHash = parseHex(record.get().sha256Hex());
            if (expectedHash.length > 0 && MessageDigest.isEqual(expectedHash, actualHash)) {
                return Optional.of(new ApiPrincipal(
                        record.get().keyId(),
                        "API_KEY",
                        record.get().roles(),
                        record.get().scopes()
                ));
            }
        }

        return Optional.empty();
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256(value));
    }

    private Optional<ApiKeyRecord> parse(String entry) {
        if (entry == null || entry.isBlank()) {
            return Optional.empty();
        }

        String[] parts = entry.trim().split(":", 4);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ApiKeyRecord(
                parts[0].trim(),
                parts[1].trim().toLowerCase(),
                parts.length >= 3 ? splitValues(parts[2]) : Set.of(),
                parts.length >= 4 ? splitValues(parts[3]) : Set.of()
        ));
    }

    private static Set<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest
                    .getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static byte[] parseHex(String value) {
        try {
            return HexFormat.of().parseHex(value);
        } catch (RuntimeException ex) {
            return new byte[0];
        }
    }

    private record ApiKeyRecord(
            String keyId,
            String sha256Hex,
            Set<String> roles,
            Set<String> scopes
    ) {
    }
}
