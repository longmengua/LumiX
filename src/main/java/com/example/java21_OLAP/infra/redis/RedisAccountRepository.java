package com.example.java21_OLAP.infra.redis;

import com.example.java21_OLAP.domain.model.Account;
import com.example.java21_OLAP.domain.repository.AccountRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Account Repository 的 Redis 實作
 * - DEMO：直接以 Java 物件存入 Redis（生產建議 JSON/二進位序列化）
 */
@Repository
public class RedisAccountRepository implements AccountRepository {

    private final RedisTemplate<String, Object> redis;

    public RedisAccountRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    private String key(long uid) {
        return "acc:" + uid;
    }

    @Override
    public Optional<Account> findByUid(long uid) {
        return Optional.ofNullable((Account) redis.opsForValue().get(key(uid)));
    }

    @Override
    public void save(Account account) {
        redis.opsForValue().set(key(account.uid()), account);
    }
}
