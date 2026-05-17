package com.example.exchange.domain.repository;

import java.time.Instant;

public interface IdempotencyRepository {

    boolean insertIfAbsent(String key, Instant expiresAt);

    boolean exists(String key);
}
