package com.lumix.withdrawal.request;
import java.util.*; import java.util.regex.Pattern;
/** 指定 withdrawal network format 後的 canonical destination；未做任何地址派發或 wallet 操作。 */
public record WithdrawalDestination(WithdrawalDestinationFormat format, String canonicalValue) {
 private static final Pattern EVM=Pattern.compile("0x[0-9a-fA-F]{40}"), BASE58=Pattern.compile("[1-9A-HJ-NP-Za-km-z]{26,128}"), BECH32=Pattern.compile("[a-z0-9]{1,83}1[ac-hj-np-z02-9]{6,87}");
 public WithdrawalDestination { format=Objects.requireNonNull(format,"format"); canonicalValue=Objects.requireNonNull(canonicalValue,"canonicalValue"); if (canonicalValue.isBlank()||!canonicalValue.equals(canonicalValue.trim())) throw new IllegalArgumentException("destination must be canonical"); }
 public static WithdrawalDestination from(String raw, WithdrawalNetwork network) { raw=Objects.requireNonNull(raw,"raw").trim(); network=Objects.requireNonNull(network,"network"); String value=switch(network.destinationFormat()){ case EVM_HEX -> { if(!EVM.matcher(raw).matches()) throw new IllegalArgumentException("invalid EVM destination"); yield raw.toLowerCase(Locale.ROOT); } case BASE58 -> {if(!BASE58.matcher(raw).matches()) throw new IllegalArgumentException("invalid BASE58 destination"); yield raw;} case BECH32 -> {if(!raw.equals(raw.toLowerCase(Locale.ROOT))||!BECH32.matcher(raw).matches()) throw new IllegalArgumentException("invalid BECH32 destination"); yield raw;} }; return new WithdrawalDestination(network.destinationFormat(),value); }
}
