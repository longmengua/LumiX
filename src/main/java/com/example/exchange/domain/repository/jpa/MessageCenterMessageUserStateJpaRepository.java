/*
 * File purpose: 訊息中心使用者狀態 repository（列表/查詢/更新）。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MessageCenterMessage;
import com.example.exchange.domain.model.entity.MessageCenterMessageUserState;
import com.example.exchange.domain.repository.MessageCenterListProjection;
import com.example.exchange.domain.repository.MessageCenterUnreadCountProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MessageCenterMessageUserStateJpaRepository
        extends JpaRepository<MessageCenterMessageUserState, MessageCenterMessageUserState.MessageStateId> {

    Optional<MessageCenterMessageUserState> findByUidAndMessageId(long uid, String messageId);

    boolean existsByUidAndDedupeKey(long uid, String dedupeKey);

    @Query("""
            select
              m.id as messageId,
              m.title as title,
              m.summary as summary,
              m.body as body,
              m.category as category,
              m.severity as severity,
              m.createdAt as createdAt,
              s.read as isRead,
              s.deleted as isDeleted,
              s.archived as isArchived,
              s.pinned as isPinned,
              m.expireAt as expireAt,
              m.scheduled as isScheduled,
              m.actionUrl as actionUrl,
              m.actionLabel as actionLabel
            from MessageCenterMessageUserState s
            join MessageCenterMessage m on m.id = s.messageId
            where s.uid = :uid
              and (:excludeDeleted = false or s.deleted = false)
              and s.archived = :archived
              and (:statusUnread = false or s.read = false)
              and (:searchPattern is null or lower(m.title) like :searchPattern or lower(m.summary) like :searchPattern or lower(m.body) like :searchPattern)
              and (:categories is null or m.category in :categories)
              and (:cursorCreatedAt is null
                    or m.createdAt < :cursorCreatedAt
                    or (m.createdAt = :cursorCreatedAt and m.id < :cursorMessageId))
            order by
              case when :pinnedFirst = true and s.pinned = true then 0 else 1 end,
              m.createdAt desc,
              m.id desc
            """)
    List<MessageCenterListProjection> findMessagesByUser(
            @Param("uid") long uid,
            @Param("statusUnread") boolean statusUnread,
            @Param("archived") boolean archived,
            @Param("categories") List<String> categories,
            @Param("searchPattern") String searchPattern,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorMessageId") String cursorMessageId,
            @Param("excludeDeleted") boolean excludeDeleted,
            @Param("pinnedFirst") boolean pinnedFirst,
            Pageable pageable
    );

    @Query("""
            select count(s)
            from MessageCenterMessageUserState s
            join MessageCenterMessage m on m.id = s.messageId
            where s.uid = :uid
              and s.read = false
              and s.deleted = false
              and (:excludeArchived = false or s.archived = false)
            """)
    long countUnread(
            @Param("uid") long uid,
            @Param("excludeArchived") boolean excludeArchived
    );

    @Query("""
            select m.category as category, count(s)
            from MessageCenterMessageUserState s
            join MessageCenterMessage m on m.id = s.messageId
            where s.uid = :uid
              and s.read = false
              and s.deleted = false
              and (:excludeArchived = false or s.archived = false)
            group by m.category
            """)
    List<MessageCenterUnreadCountProjection> countUnreadByCategory(
            @Param("uid") long uid,
            @Param("excludeArchived") boolean excludeArchived
    );

    @Query("""
            select count(s)
            from MessageCenterMessageUserState s
            join MessageCenterMessage m on m.id = s.messageId
            where s.uid = :uid
              and s.read = false
              and s.deleted = false
              and s.archived = :archived
              and (:categories is null or m.category in :categories)
            """)
    long countUnreadByFilter(
            @Param("uid") long uid,
            @Param("archived") boolean archived,
            @Param("categories") Collection<String> categories
    );

    @Query("""
            select s
            from MessageCenterMessageUserState s
            join MessageCenterMessage m on m.id = s.messageId
            where s.uid = :uid
              and s.read = false
              and s.deleted = false
              and s.archived = :archived
              and (:categories is null or m.category in :categories)
            """)
    List<MessageCenterMessageUserState> findUnreadForReadAll(
            @Param("uid") long uid,
            @Param("archived") boolean archived,
            @Param("categories") Collection<String> categories
    );

    @Modifying
    @Transactional
    @Query("""
            update MessageCenterMessageUserState s
            set s.archived = true,
                s.updatedAt = :now
            where s.uid = :uid and s.messageId = :messageId
            """)
    int archive(
            @Param("uid") long uid,
            @Param("messageId") String messageId,
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query("""
            update MessageCenterMessageUserState s
            set s.archived = false,
                s.updatedAt = :now
            where s.uid = :uid and s.messageId = :messageId
            """)
    int unarchive(
            @Param("uid") long uid,
            @Param("messageId") String messageId,
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query("""
            update MessageCenterMessageUserState s
            set s.deleted = true,
                s.updatedAt = :now
            where s.uid = :uid and s.messageId = :messageId
            """)
    int softDelete(
            @Param("uid") long uid,
            @Param("messageId") String messageId,
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query("""
            update MessageCenterMessageUserState s
            set s.pinned = true,
                s.pinAt = :pinAt,
                s.updatedAt = :now
            where s.uid = :uid and s.messageId = :messageId
            """)
    int pin(
            @Param("uid") long uid,
            @Param("messageId") String messageId,
            @Param("pinAt") Instant pinAt,
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query("""
            update MessageCenterMessageUserState s
            set s.pinned = false,
                s.pinAt = null,
                s.updatedAt = :now
            where s.uid = :uid and s.messageId = :messageId
            """)
    int unpin(
            @Param("uid") long uid,
            @Param("messageId") String messageId,
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query("""
            update MessageCenterMessageUserState s
            set s.read = true,
                s.readAt = :readAt,
                s.updatedAt = :readAt
            where s.uid = :uid and s.messageId = :messageId and s.read = false and s.deleted = false
            """)
    int markRead(
            @Param("uid") long uid,
            @Param("messageId") String messageId,
            @Param("readAt") Instant readAt
    );
}
