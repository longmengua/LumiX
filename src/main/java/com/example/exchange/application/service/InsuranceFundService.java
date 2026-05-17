package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InsuranceFundService {

    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final List<AdlQueueEntry> adlQueue = new ArrayList<>();

    public synchronized BigDecimal balance(String asset) {
        return balances.computeIfAbsent(normalize(asset), ignored -> DEFAULT_BALANCE);
    }

    public synchronized BigDecimal cover(String asset, BigDecimal requested) {
        if (requested == null || requested.signum() <= 0) return BigDecimal.ZERO;
        String key = normalize(asset);
        BigDecimal current = balance(key);
        BigDecimal covered = current.min(requested);
        balances.put(key, current.subtract(covered));
        return covered;
    }

    public synchronized void enqueueAdl(
            String liquidationId,
            long uid,
            String symbol,
            BigDecimal amount
    ) {
        if (amount == null || amount.signum() <= 0) return;
        adlQueue.add(new AdlQueueEntry(liquidationId, uid, normalize(symbol), amount, Instant.now()));
    }

    public synchronized List<AdlQueueEntry> adlQueue() {
        return List.copyOf(adlQueue);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
