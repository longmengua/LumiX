package com.lumix.withdrawal.risk;

import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 最低安全規則組；資料不足不得偽造證據，未知資料由上游導向 HOLD。 */
@Configuration
public class WithdrawalSecurityRiskRules {
    @Bean WithdrawalRiskRule accountWithdrawalFrozenRule() {
        return context -> context.withdrawalFrozenAt()!=null ? result("ACCOUNT_WITHDRAWAL_FROZEN",100,RiskDecision.DENY) : result("ACCOUNT_WITHDRAWAL_FROZEN",0,RiskDecision.ALLOW);
    }
    @Bean WithdrawalRiskRule newDeviceRestrictionRule() {
        Clock clock=Clock.systemUTC();
        return context -> context.transferRestrictedUntil()!=null && Instant.now(clock).isBefore(context.transferRestrictedUntil()) ? result("NEW_DEVICE_RESTRICTION",80,RiskDecision.DENY) : result("NEW_DEVICE_RESTRICTION",0,RiskDecision.ALLOW);
    }
    @Bean WithdrawalRiskRule recentPasswordChangeRule(WithdrawalPlatformProperties properties) {
        Clock clock=Clock.systemUTC();
        return context -> context.passwordChangedAt()!=null && Instant.now(clock).isBefore(context.passwordChangedAt().plus(properties.getPasswordChangeCooldown())) ? result("RECENT_PASSWORD_CHANGE",30,RiskDecision.MANUAL_REVIEW) : result("RECENT_PASSWORD_CHANGE",0,RiskDecision.ALLOW);
    }
    @Bean WithdrawalRiskRule newDeviceRule() { return context -> !context.trustedDevice() ? result("NEW_DEVICE",15,RiskDecision.MANUAL_REVIEW) : result("NEW_DEVICE",0,RiskDecision.ALLOW); }
    @Bean WithdrawalRiskRule newIpRule() { return context -> context.newIp() ? result("NEW_IP",5,RiskDecision.MANUAL_REVIEW) : result("NEW_IP",0,RiskDecision.ALLOW); }
    @Bean WithdrawalRiskRule highSingleAmountRule(WithdrawalPlatformProperties properties) { return context -> context.amount().compareTo(properties.getManualReviewAmount()) >= 0 ? result("HIGH_SINGLE_AMOUNT",20,RiskDecision.MANUAL_REVIEW) : result("HIGH_SINGLE_AMOUNT",0,RiskDecision.ALLOW); }
    /** velocity backend 缺席或不可用時是風控 evidence gap，故一律人工審核。 */
    @Bean WithdrawalRiskRule velocityRule(WithdrawalPlatformProperties properties,ObjectProvider<WithdrawalVelocityService> velocities) { return context -> { WithdrawalVelocityService service=velocities.getIfAvailable(); if(service==null)return result("WITHDRAWAL_VELOCITY_UNKNOWN",0,RiskDecision.MANUAL_REVIEW); try { WithdrawalVelocityService.Snapshot value=service.snapshot(context.userId(),context.deviceId(),context.ipAddress()); boolean exceeded=value.tenMinuteCount()>=properties.getTenMinuteCount()||value.oneHourCount()>=properties.getOneHourCount()||value.dailyCount()>=properties.getDailyCount()||value.dailyAmount().add(context.amount()).compareTo(properties.getDailyAmount())>0||value.deviceOneHourCount()>=properties.getOneHourCount()||value.ipOneHourCount()>=properties.getOneHourCount(); return exceeded?result("WITHDRAWAL_VELOCITY_LIMIT",25,RiskDecision.MANUAL_REVIEW):result("WITHDRAWAL_VELOCITY",0,RiskDecision.ALLOW); }catch(WithdrawalVelocityService.VelocityUnavailableException exception){return result("WITHDRAWAL_VELOCITY_UNAVAILABLE",0,RiskDecision.MANUAL_REVIEW);} }; }
    private static RiskRuleResult result(String code,int score,RiskDecision decision) { return new RiskRuleResult(code,score,decision,Map.of("evaluated",true)); }
}
