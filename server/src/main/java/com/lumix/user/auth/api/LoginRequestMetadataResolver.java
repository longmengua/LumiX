package com.lumix.user.auth.api;

import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.BoundDevicePlatform;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 將 servlet request 轉為不保存完整 User-Agent 的登入安全快照。
 *
 * <p>fingerprint 結合 User-Agent、Client Hints 與語系，完成 pending login 時必須再次相符。它能阻止
 * 單獨複製 cookie 後的普通跨瀏覽器重放，但 browser header 仍可能遭高能力攻擊者模擬，所以不可取代一次性
 * 高熵秘密、短時效與原子消耗。</p>
 */
final class LoginRequestMetadataResolver {

    private LoginRequestMetadataResolver() { }

    static LoginRequestMetadata resolve(HttpServletRequest request) {
        String userAgent = boundedHeader(request.getHeader("User-Agent"), "Unknown browser", 512);
        String fingerprintInput = String.join("\n",
            userAgent,
            boundedHeader(request.getHeader("Sec-CH-UA"), "", 512),
            boundedHeader(request.getHeader("Sec-CH-UA-Platform"), "", 128),
            boundedHeader(request.getHeader("Sec-CH-UA-Mobile"), "", 32),
            boundedHeader(request.getHeader("Accept-Language"), "", 256)
        );
        return new LoginRequestMetadata(
            boundedText(request.getRemoteAddr(), "Unknown IP", 64),
            describeDevice(userAgent), digest(fingerprintInput), classifyPlatform(userAgent)
        );
    }

    private static String boundedHeader(String value, String fallback, int maximumLength) {
        return boundedText(value, fallback, maximumLength);
    }

    private static String boundedText(String value, String fallback, int maximumLength) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.replaceAll("[\\r\\n]", " ").trim();
        return normalized.length() > maximumLength ? normalized.substring(0, maximumLength) : normalized;
    }

    private static String describeDevice(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        String platform = normalized.contains("iphone") ? "iPhone"
            : normalized.contains("ipad") ? "iPad"
            : normalized.contains("android") ? "Android"
            : normalized.contains("windows") ? "Windows"
            : normalized.contains("mac os") ? "macOS"
            : normalized.contains("linux") ? "Linux" : "Unknown platform";
        String browser = normalized.contains("edg/") ? "Microsoft Edge"
            : normalized.contains("firefox/") ? "Firefox"
            : normalized.contains("chrome/") || normalized.contains("crios/") ? "Chrome"
            : normalized.contains("safari/") ? "Safari" : "Unknown browser";
        return browser + " on " + platform;
    }

    /**
     * 只以 UA 的粗粒度訊號分配三個產品槽位。
     *
     * <p>UA 可遭偽造，因此真正信任仍由 device cookie、摘要比對與 email 一次性核准維持；未知 UA 落到
     * 桌電槽位，確保不會產生無上限的第四種裝置。</p>
     */
    private static BoundDevicePlatform classifyPlatform(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("ipad") || normalized.contains("tablet")
            || (normalized.contains("android") && !normalized.contains("mobile"))) {
            return BoundDevicePlatform.TABLET;
        }
        if (normalized.contains("iphone") || normalized.contains("ipod") || normalized.contains("mobile")
            || normalized.contains("windows phone")) {
            return BoundDevicePlatform.MOBILE;
        }
        return BoundDevicePlatform.DESKTOP;
    }

    private static String digest(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
