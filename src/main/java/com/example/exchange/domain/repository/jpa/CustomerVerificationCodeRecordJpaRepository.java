/*
 * File purpose: JPA repository for independent customer verification code rows.
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.CustomerVerificationCodeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerVerificationCodeRecordJpaRepository extends JpaRepository<CustomerVerificationCodeRecord, Long> {

    Optional<CustomerVerificationCodeRecord> findFirstByEmailAndStatusOrderByCreatedAtDesc(String email, String status);
}
