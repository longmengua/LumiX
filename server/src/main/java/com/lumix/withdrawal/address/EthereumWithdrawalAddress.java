package com.lumix.withdrawal.address;

import java.util.Locale;
import java.util.regex.Pattern;

/** Ethereum 地址格式與一致化；不接受前端提供的 token contract 或 chain id。 */
public final class EthereumWithdrawalAddress {
    private static final Pattern FORMAT = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private EthereumWithdrawalAddress() { }
    public static String normalize(String address) {
        if (address == null || !FORMAT.matcher(address.trim()).matches()) throw new IllegalArgumentException("invalid Ethereum address");
        // 對未實作 EIP-55 checksum 的輸入保存小寫 identity，避免大小寫導致同地址重複新增。
        return address.trim().toLowerCase(Locale.ROOT);
    }
}
