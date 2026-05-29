/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.domain.repository.WalletTransferRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class RedisWalletTransferRepository implements WalletTransferRepository {

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisWalletTransferRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    /**
     * 保存 transfer 本體，並維護 uid -> transfer ids 的 list/set 索引。
     * set 用於去重，list 用於保留第一次寫入的查詢順序。
     */
    @Override
    public void save(WalletTransfer transfer) {
        String id = transfer.getId().toString();
        redis.opsForValue().set(transferKey(transfer.getId()), transfer);
        if (transfer.getExternalRef() != null && !transfer.getExternalRef().isBlank()) {
            redis.opsForValue().set(externalRefKey(transfer.getExternalRef()), id);
        }
        Long added = redis.opsForSet().add(uidSetKey(transfer.getUid()), id);
        if (added != null && added > 0) {
            redis.opsForList().rightPush(uidListKey(transfer.getUid()), id);
        }
    }

    /** 直接透過 transfer key 查詢單筆狀態。 */
    @Override
    public Optional<WalletTransfer> findById(UUID id) {
        Object value = redis.opsForValue().get(transferKey(id));
        return value instanceof WalletTransfer transfer ? Optional.of(transfer) : Optional.empty();
    }

    /** 透過外部 callback reference 查詢，避免鏈上 / 銀行 callback 重送重複入帳。 */
    @Override
    public Optional<WalletTransfer> findByExternalRef(String externalRef) {
        if (externalRef == null || externalRef.isBlank()) {
            return Optional.empty();
        }
        Object rawId = redis.opsForValue().get(externalRefKey(externalRef));
        if (rawId == null) {
            return Optional.empty();
        }
        try {
            return findById(UUID.fromString(String.valueOf(rawId)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** 透過 uid list index 批量載入 transfer，避免使用 Redis KEYS 掃描。 */
    @Override
    public List<WalletTransfer> findByUid(long uid) {
        List<Object> rawIds = redis.opsForList().range(uidListKey(uid), 0, -1);
        if (rawIds == null || rawIds.isEmpty()) return List.of();
        List<String> ids = rawIds.stream().map(String::valueOf).toList();
        List<String> redisKeys = ids.stream()
                .map(id -> keys.key("wallet:transfer:" + id))
                .collect(Collectors.toList());
        ValueOperations<String, Object> ops = redis.opsForValue();
        List<Object> values = ops.multiGet(redisKeys);
        if (values == null) return List.of();

        Map<String, WalletTransfer> transfers = new LinkedHashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            Object value = values.size() > index ? values.get(index) : null;
            if (value instanceof WalletTransfer transfer) {
                transfers.put(ids.get(index), transfer);
            }
        }
        return new ArrayList<>(transfers.values());
    }

    private String transferKey(UUID id) {
        return keys.key("wallet:transfer:" + id);
    }

    private String uidListKey(long uid) {
        return keys.key("wallet:transfer:list:" + uid);
    }

    private String uidSetKey(long uid) {
        return keys.key("wallet:transfer:set:" + uid);
    }

    private String externalRefKey(String externalRef) {
        return keys.key("wallet:transfer:external-ref:" + externalRef.trim());
    }
}
