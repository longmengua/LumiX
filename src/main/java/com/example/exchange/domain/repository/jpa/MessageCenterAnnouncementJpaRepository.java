/*
 * File purpose: 訊息公告 repository。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MessageCenterAnnouncement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageCenterAnnouncementJpaRepository
        extends JpaRepository<MessageCenterAnnouncement, String> {

    @Query("""
            select a
            from MessageCenterAnnouncement a
            where (:status is null or a.status = :status)
              and (:category is null or a.category = :category)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
              and (:cursorCreatedAt is null
                    or a.createdAt < :cursorCreatedAt
                    or (a.createdAt = :cursorCreatedAt and a.id < :cursorMessageId))
            order by a.createdAt desc, a.id desc
            """)
    List<MessageCenterAnnouncement> findAnnouncements(
            @Param("status") String status,
            @Param("category") String category,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorMessageId") String cursorMessageId,
            Pageable pageable
    );

    @Query("""
            select a
            from MessageCenterAnnouncement a
            where a.status = :status
              and a.sendAt <= :now
            order by a.sendAt asc, a.id asc
            """)
    List<MessageCenterAnnouncement> findReadyScheduled(@Param("status") String status, @Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MessageCenterAnnouncement a where a.id = :announcementId")
    Optional<MessageCenterAnnouncement> lockByIdForUpdate(@Param("announcementId") String announcementId);
}
