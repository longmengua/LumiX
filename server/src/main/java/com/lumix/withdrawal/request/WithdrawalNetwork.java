package com.lumix.withdrawal.request;
import java.util.Locale;
import java.util.Objects;
/** withdrawal domain 的 network identity；避免依賴入金 address module。 */
public record WithdrawalNetwork(String code, WithdrawalDestinationFormat destinationFormat) {
 public WithdrawalNetwork { code = Objects.requireNonNull(code,"code").trim().toUpperCase(Locale.ROOT); destinationFormat=Objects.requireNonNull(destinationFormat,"destinationFormat"); if (!code.matches("[A-Z0-9_:-]{2,64}")) throw new IllegalArgumentException("withdrawal network code must be canonical"); }
}
