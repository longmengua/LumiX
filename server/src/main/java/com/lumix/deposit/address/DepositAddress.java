package com.lumix.deposit.address;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 指定 network format 後的 canonical address 字串。
 *
 * <p>只在 format 明確定義大小寫無關時才做 normalization；未知格式不會被自動 trim/轉碼成看似相同的地址。</p>
 */
public record DepositAddress(DepositAddressFormat format, String canonicalValue) {

    private static final Pattern EVM = Pattern.compile("0x[0-9a-fA-F]{40}");
    private static final Pattern BASE58 = Pattern.compile("[1-9A-HJ-NP-Za-km-z]{26,128}");
    private static final Pattern BECH32 = Pattern.compile("[a-z0-9]{1,83}1[ac-hj-np-z02-9]{6,87}");

    public DepositAddress {
        format = Objects.requireNonNull(format, "format must not be null");
        canonicalValue = Objects.requireNonNull(canonicalValue, "canonicalValue must not be null");
        if (canonicalValue.isBlank() || !canonicalValue.equals(canonicalValue.trim())) {
            throw new IllegalArgumentException("deposit address must be non-blank and already trimmed");
        }
    }

    /**
     * 依 network format 產生可比較的 canonical form；無效或混合大小寫 Bech32 一律拒絕而不猜測意圖。
     */
    public static DepositAddress from(String rawValue, DepositNetwork network) {
        rawValue = Objects.requireNonNull(rawValue, "rawValue must not be null").trim();
        network = Objects.requireNonNull(network, "network must not be null");
        String canonical = switch (network.addressFormat()) {
            case EVM_HEX -> normalizeEvm(rawValue);
            case BASE58 -> requireBase58(rawValue);
            case BECH32 -> normalizeBech32(rawValue);
        };
        return new DepositAddress(network.addressFormat(), canonical);
    }

    private static String normalizeEvm(String value) {
        if (!EVM.matcher(value).matches()) {
            throw new IllegalArgumentException("EVM address must be 0x plus 40 hexadecimal characters");
        }
        // 未導入 checksum provider 時，以小寫作唯一可重放的 identity；不宣稱 checksum 驗證已完成。
        return value.toLowerCase(Locale.ROOT);
    }

    private static String requireBase58(String value) {
        if (!BASE58.matcher(value).matches()) {
            throw new IllegalArgumentException("BASE58 address must use a valid canonical character set and length");
        }
        return value;
    }

    private static String normalizeBech32(String value) {
        if (!value.equals(value.toLowerCase(Locale.ROOT)) || !BECH32.matcher(value).matches()) {
            throw new IllegalArgumentException("BECH32 address must be lowercase canonical form");
        }
        return value;
    }
}
