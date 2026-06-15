/*
 * File purpose: 訊息偏好 repository（依使用者與分類儲存推播渠道開關）。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MessageCenterNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageCenterNotificationPreferenceJpaRepository
        extends JpaRepository<MessageCenterNotificationPreference, MessageCenterNotificationPreference.PreferenceId> {

    Optional<MessageCenterNotificationPreference> findByUidAndCategory(long uid, String category);

    List<MessageCenterNotificationPreference> findByUid(long uid);
}
