package com.lumix.withdrawal.risk;

import java.math.BigDecimal;
import java.time.Instant;

/** 已去敏的提款風控輸入快照；不得把 password、session secret 或 cookie 放入 evidence。 */
public record WithdrawalRiskContext(String userId, BigDecimal amount, Instant passwordChangedAt,
                                    Instant transferRestrictedUntil, Instant withdrawalFrozenAt,
                                    boolean trustedDevice, boolean newIp, String ipAddress, String deviceId) { }
