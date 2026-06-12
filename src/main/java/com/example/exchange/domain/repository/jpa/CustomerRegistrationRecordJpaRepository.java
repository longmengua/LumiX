/*
 * File purpose: JPA repository for pending customer registration verification requests.
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.CustomerRegistrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRegistrationRecordJpaRepository extends JpaRepository<CustomerRegistrationRecord, Long> {

    Optional<CustomerRegistrationRecord> findFirstByEmailAndStatusOrderByCreatedAtDesc(String email, String status);

    Optional<CustomerRegistrationRecord> findByVerificationTokenHashAndStatus(String verificationTokenHash, String status);
}
