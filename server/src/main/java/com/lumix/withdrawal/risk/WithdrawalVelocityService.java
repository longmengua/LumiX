package com.lumix.withdrawal.risk;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** Redis 只提供短期 velocity signal；withdrawals 資料表仍是唯一財務權威。 */
@Service @Profile("infrastructure")
public class WithdrawalVelocityService {
    private final RedisTemplate<String,String> redis;
    private static final DefaultRedisScript<String> AMOUNT_INCREMENT = new DefaultRedisScript<>("local v=redis.call('INCRBYFLOAT',KEYS[1],ARGV[1]); if redis.call('TTL',KEYS[1]) < 0 then redis.call('EXPIRE',KEYS[1],ARGV[2]) end; return v",String.class);
    public WithdrawalVelocityService(RedisTemplate<String,String> redis){this.redis=Objects.requireNonNull(redis);}
    /** 讀取失敗一律拋出，caller 必須導向 MANUAL_REVIEW 而不可假設 0。 */
    public Snapshot snapshot(String userId,String deviceId,String ip){try{return new Snapshot(count(user(userId,"10m:count")),count(user(userId,"1h:count")),count(user(userId,"24h:count")),amount(user(userId,"24h:amount")),count(device(deviceId,"1h:count")),count(ip(ip,"1h:count")));}catch(RuntimeException e){throw new VelocityUnavailableException(e);}}
    /** 在 request 已持久化後記錄；操作失敗不回寫 financial truth，僅讓後續風控採 fail-safe。 */
    public void record(String userId,String deviceId,String ip,BigDecimal amount){try{increment(user(userId,"10m:count"),Duration.ofMinutes(10));increment(user(userId,"1h:count"),Duration.ofHours(1));increment(user(userId,"24h:count"),Duration.ofHours(24));if(deviceId!=null)increment(device(deviceId,"1h:count"),Duration.ofHours(1));increment(ip(ip,"1h:count"),Duration.ofHours(1));redis.execute(AMOUNT_INCREMENT,java.util.List.of(user(userId,"24h:amount")),amount.toPlainString(),Long.toString(Duration.ofHours(24).toSeconds()));}catch(RuntimeException e){throw new VelocityUnavailableException(e);}}
    private long count(String key){String value=redis.opsForValue().get(key);return value==null?0:Long.parseLong(value);}
    private BigDecimal amount(String key){String value=redis.opsForValue().get(key);return value==null?BigDecimal.ZERO:new BigDecimal(value);}
    private void increment(String key,Duration ttl){Long value=redis.opsForValue().increment(key);if(value!=null&&value==1L)redis.expire(key,ttl);}
    private static String user(String id,String suffix){return "risk:withdrawal:user:"+id+":"+suffix;}
    private static String device(String id,String suffix){return "risk:withdrawal:device:"+(id==null?"unknown":hash(id))+":"+suffix;}
    private static String ip(String value,String suffix){return "risk:withdrawal:ip:"+hash(value)+":"+suffix;}
    private static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}}
    public record Snapshot(long tenMinuteCount,long oneHourCount,long dailyCount,BigDecimal dailyAmount,long deviceOneHourCount,long ipOneHourCount) { }
    public static final class VelocityUnavailableException extends RuntimeException { VelocityUnavailableException(Throwable cause){super("withdrawal velocity is unavailable",cause);} }
}
