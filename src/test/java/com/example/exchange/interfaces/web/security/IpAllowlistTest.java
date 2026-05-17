/*
 * 檔案用途：測試 IP 白名單工具，涵蓋精確 IP、IPv4 CIDR 與空白規則。
 */
package com.example.exchange.interfaces.web.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IpAllowlistTest {

    @Test
    void allowsExactIpAndWildcard() {
        assertThat(IpAllowlist.allows("127.0.0.1", List.of("127.0.0.1")))
                .isTrue();
        assertThat(IpAllowlist.allows("203.0.113.9", List.of("*")))
                .isTrue();
    }

    @Test
    void allowsIpv4Cidr() {
        assertThat(IpAllowlist.allows("10.8.1.3", List.of("10.0.0.0/8")))
                .isTrue();
        assertThat(IpAllowlist.allows("192.168.10.20", List.of("192.168.10.0/24")))
                .isTrue();
    }

    @Test
    void rejectsOutsideRules() {
        assertThat(IpAllowlist.allows("172.16.0.1", List.of("10.0.0.0/8")))
                .isFalse();
        assertThat(IpAllowlist.allows("10.0.0.1", List.of("example.com/32")))
                .isFalse();
        assertThat(IpAllowlist.allows("127.0.0.1", List.of()))
                .isFalse();
        assertThat(IpAllowlist.allows("", List.of("127.0.0.1")))
                .isFalse();
    }
}
