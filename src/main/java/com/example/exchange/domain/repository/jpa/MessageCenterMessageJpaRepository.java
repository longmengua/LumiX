/*
 * File purpose: 訊息中心訊息主檔 repository。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MessageCenterMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageCenterMessageJpaRepository
        extends JpaRepository<MessageCenterMessage, String> {

    Optional<MessageCenterMessage> findFirstByDedupeKey(String dedupeKey);
}
