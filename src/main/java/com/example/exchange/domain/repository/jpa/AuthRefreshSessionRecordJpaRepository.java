/*
 * File purpose: JPA repository for local auth refresh sessions.
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.AuthRefreshSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRefreshSessionRecordJpaRepository extends JpaRepository<AuthRefreshSessionRecord, Long> {

    Optional<AuthRefreshSessionRecord> findByRefreshTokenHash(String refreshTokenHash);
}
