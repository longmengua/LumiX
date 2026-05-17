/*
 * 檔案用途：測試 IP 白名單工具，涵蓋精確 IP、IPv4 CIDR 與空白規則。
 */
package com.example.exchange.interfaces.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋 IP allowlist helper 支援的規則型態：精確 IP、萬用字元、IPv4 CIDR 與拒絕分支。
 */
class IpAllowlistTest {

    @Test
    @DisplayName("精確 IP 與萬用字元規則會放行")
    void allowsExactIpAndWildcard() {
        assertThat(IpAllowlist.allows("127.0.0.1", List.of("127.0.0.1")))
                .isTrue();
        assertThat(IpAllowlist.allows("203.0.113.9", List.of("*")))
                .isTrue();
    }

    @Test
    @DisplayName("IPv4 CIDR 規則會放行網段內 IP")
    void allowsIpv4Cidr() {
        assertThat(IpAllowlist.allows("10.8.1.3", List.of("10.0.0.0/8")))
                .isTrue();
        assertThat(IpAllowlist.allows("192.168.10.20", List.of("192.168.10.0/24")))
                .isTrue();
    }

    @Test
    @DisplayName("網段外、無效規則、空規則與空 IP 都拒絕")
    void rejectsOutsideRules() {
        assertThat(IpAllowlist.allows("172.16.0.1", List.of("10.0.0.0/8")))
                .isFalse();
        // example.com/32 不是合法 IPv4 CIDR，應被當成不匹配規則。
        assertThat(IpAllowlist.allows("10.0.0.1", List.of("example.com/32")))
                .isFalse();
        assertThat(IpAllowlist.allows("127.0.0.1", List.of()))
                .isFalse();
        assertThat(IpAllowlist.allows("", List.of("127.0.0.1")))
                .isFalse();
    }
}
