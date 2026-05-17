package com.example.exchange.application.service;

import com.example.exchange.domain.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final IdempotencyRepository repository;

    public boolean markProcessed(String key) {
        return repository.insertIfAbsent(key, Instant.now().plus(DEFAULT_TTL));
    }

    public boolean isProcessed(String key) {
        return repository.exists(key);
    }
}
