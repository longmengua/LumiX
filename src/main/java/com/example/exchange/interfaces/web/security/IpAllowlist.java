/*
 * 檔案用途：Web 安全工具，判斷 client IP 是否符合精確 IP 或 IPv4 CIDR 白名單。
 */
package com.example.exchange.interfaces.web.security;

import java.util.List;

public final class IpAllowlist {

    private IpAllowlist() {
    }

    public static boolean allows(String clientIp, List<String> allowlist) {
        if (clientIp == null || clientIp.isBlank() || allowlist == null || allowlist.isEmpty()) {
            return false;
        }

        String normalizedClientIp = clientIp.trim();

        for (String rule : allowlist) {
            if (rule == null || rule.isBlank()) {
                continue;
            }

            String normalizedRule = rule.trim();
            if ("*".equals(normalizedRule)) {
                return true;
            }

            if (normalizedRule.contains("/")) {
                if (matchesIpv4Cidr(normalizedClientIp, normalizedRule)) {
                    return true;
                }
                continue;
            }

            if (normalizedRule.equalsIgnoreCase(normalizedClientIp)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesIpv4Cidr(String clientIp, String cidrRule) {
        String[] parts = cidrRule.split("/", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            int prefixLength = Integer.parseInt(parts[1]);
            if (prefixLength < 0 || prefixLength > 32) {
                return false;
            }

            long client = ipv4ToLong(clientIp);
            long network = ipv4ToLong(parts[0]);
            long mask = prefixLength == 0
                    ? 0
                    : 0xFFFF_FFFFL << (32 - prefixLength) & 0xFFFF_FFFFL;

            return (client & mask) == (network & mask);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static long ipv4ToLong(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            throw new IllegalArgumentException("not ipv4");
        }

        long result = 0;
        for (String octet : octets) {
            int part = Integer.parseInt(octet);
            if (part < 0 || part > 255) {
                throw new IllegalArgumentException("invalid ipv4 octet");
            }
            result = (result << 8) | part;
        }
        return result;
    }
}
