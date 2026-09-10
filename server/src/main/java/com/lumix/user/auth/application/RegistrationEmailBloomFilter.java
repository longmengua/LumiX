package com.lumix.user.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 註冊 email 的 Redis Bloom filter。
 *
 * <p>它只可用來快速判斷「可能存在」；Bloom false positive 不能拒絕註冊，仍必須查 PostgreSQL 與依賴
 * unique constraint 最終裁決。Redis 不可用時一律回退資料庫路徑，不能使註冊不可用。</p>
 */
@Component
@Profile("infrastructure")
public class RegistrationEmailBloomFilter {

    private static final String KEY = "lumix:auth:registration-email-bloom:v1";
    private static final long BIT_SIZE = 16_777_216L;
    private static final int HASH_COUNT = 6;
    private final RedisTemplate<String, String> redisTemplate;

    public RegistrationEmailBloomFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Redis 失敗採 false，讓 caller 走安全但較慢的資料庫 insert／unique constraint 路徑。 */
    public boolean mightContain(String normalizedEmail) {
        try {
            for (long offset : offsets(normalizedEmail)) {
                if (!Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(KEY, offset))) return false;
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** 成功寫入帳號後才加入 filter，避免 failed transaction 製造不必要的可能存在訊號。 */
    public void add(String normalizedEmail) {
        try {
            for (long offset : offsets(normalizedEmail)) redisTemplate.opsForValue().setBit(KEY, offset, true);
        } catch (RuntimeException ignored) {
            // Bloom 是效能最佳化；不可讓 cache 故障破壞 PostgreSQL 的正確性。
        }
    }

    private static long[] offsets(String value) {
        byte[] digest = sha256(value);
        long[] offsets = new long[HASH_COUNT];
        for (int index = 0; index < HASH_COUNT; index++) {
            long hash = 0;
            for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
                hash = (hash << 8) | (digest[(index * 4 + byteIndex) % digest.length] & 0xffL);
            }
            offsets[index] = Long.remainderUnsigned(hash, BIT_SIZE);
        }
        return offsets;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
