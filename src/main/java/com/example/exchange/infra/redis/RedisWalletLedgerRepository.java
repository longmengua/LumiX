/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class RedisWalletLedgerRepository implements WalletLedgerRepository {

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisWalletLedgerRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    @Override
    public void append(WalletLedgerEntry entry) {
        if (entry == null || !entry.isBalanced()) {
            throw new IllegalArgumentException("wallet ledger entry must be balanced");
        }
        String id = entry.getId().toString();
        redis.opsForValue().set(entryKey(id), entry);
        redis.opsForList().rightPush(uidKey(entry.getUid()), id);
        if (entry.getRefId() != null && !entry.getRefId().isBlank()) {
            redis.opsForList().rightPush(refKey(entry.getRefId()), id);
        }
    }

    @Override
    public List<WalletLedgerEntry> findByUid(long uid) {
        return load(uidKey(uid));
    }

    @Override
    public List<WalletLedgerEntry> findByRefId(String refId) {
        if (refId == null || refId.isBlank()) return List.of();
        return load(refKey(refId));
    }

    private List<WalletLedgerEntry> load(String indexKey) {
        List<Object> ids = redis.opsForList().range(indexKey, 0, -1);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(String::valueOf)
                .map(id -> redis.opsForValue().get(entryKey(id)))
                .filter(WalletLedgerEntry.class::isInstance)
                .map(WalletLedgerEntry.class::cast)
                .filter(Objects::nonNull)
                .toList();
    }

    private String entryKey(String id) {
        return keys.key("wallet:ledger:" + id);
    }

    private String uidKey(long uid) {
        return keys.key("wallet:ledger:uid:" + uid);
    }

    private String refKey(String refId) {
        return keys.key("wallet:ledger:ref:" + refId);
    }
}
