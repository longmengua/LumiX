/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.dto.Account;
import com.example.exchange.domain.repository.AccountRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Account Repository 的 Redis 實作
 * - DEMO：直接以 Java 物件存入 Redis（生產建議 JSON/二進位序列化）
 */
@Repository
public class RedisAccountRepository implements AccountRepository {

    private static final String INDEX_KEY = "acc:index";

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisAccountRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    private String key(long uid) {
        return keys.key("acc:" + uid);
    }

    @Override
    public Optional<Account> findByUid(long uid) {
        return Optional.ofNullable((Account) redis.opsForValue().get(key(uid)));
    }

    @Override
    public List<Account> findAll() {
        Set<Object> uids = redis.opsForSet().members(keys.key(INDEX_KEY));
        if (uids == null || uids.isEmpty()) return List.of();
        return uids.stream()
                .map(String::valueOf)
                .map(this::parseUid)
                .flatMap(Optional::stream)
                .map(this::findByUid)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public void save(Account account) {
        redis.opsForValue().set(key(account.uid()), account);
        redis.opsForSet().add(keys.key(INDEX_KEY), String.valueOf(account.uid()));
    }

    private Optional<Long> parseUid(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException ex) {
            redis.opsForSet().remove(keys.key(INDEX_KEY), raw);
            return Optional.empty();
        }
    }
}
