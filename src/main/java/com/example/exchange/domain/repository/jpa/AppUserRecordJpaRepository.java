/*
 * File purpose: JPA repository for local exchange users.
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.AppUserRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRecordJpaRepository extends JpaRepository<AppUserRecord, Long> {

    Optional<AppUserRecord> findByEmail(String email);

    Optional<AppUserRecord> findByEmailVerificationTokenHash(String emailVerificationTokenHash);

    boolean existsByEmail(String email);
}
