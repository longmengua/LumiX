package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.AppUserRecord;
import com.example.exchange.domain.model.entity.MessageCenterAnnouncement;
import com.example.exchange.domain.model.entity.MessageCenterMessage;
import com.example.exchange.domain.model.entity.MessageCenterMessageUserState;
import com.example.exchange.domain.model.entity.MessageCenterNotificationPreference;
import com.example.exchange.domain.model.enums.MessageAudienceType;
import com.example.exchange.domain.model.enums.MessageAnnouncementStatus;
import com.example.exchange.domain.model.enums.MessageCategory;
import com.example.exchange.domain.model.enums.MessageDeliveryMode;
import com.example.exchange.domain.model.enums.MessageSeverity;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.domain.repository.MessageCenterListProjection;
import com.example.exchange.domain.repository.MessageCenterUnreadCountProjection;
import com.example.exchange.domain.repository.jpa.AppUserRecordJpaRepository;
import com.example.exchange.domain.repository.jpa.MessageCenterAnnouncementJpaRepository;
import com.example.exchange.domain.repository.jpa.MessageCenterMessageJpaRepository;
import com.example.exchange.domain.repository.jpa.MessageCenterMessageUserStateJpaRepository;
import com.example.exchange.domain.repository.jpa.MessageCenterNotificationPreferenceJpaRepository;
import com.example.exchange.interfaces.web.dto.MessageCenterUserDtos;
import com.example.exchange.interfaces.web.dto.MessageCenterWebSocketEvent;
import com.example.exchange.interfaces.web.exception.BusinessErrorCode;
import com.example.exchange.interfaces.web.exception.BusinessException;
import com.example.exchange.interfaces.web.security.UserStreamSubscriptionAuthorizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Application service for 訊息中心。
 *
 * 職責：
 * 1) 訊息列表與訊息狀態管理（read/archive/delete/pin）
 * 2) 使用者偏好管理
 * 3) 後台/系統發訊與公告排程落地
 * 4) WebSocket 即時推播事件發送
 */
@Service
public class MessageCenterService {

    private static final int DEFAULT_PAGE_LIMIT = 20;
    private static final int MAX_PAGE_LIMIT = 100;
    private static final int BATCH_USER_SIZE = 300;
    private static final String CURSOR_SEPARATOR = "|";
    private static final String DEFAULT_SUBJECT = "system";
    private static final String DEFAULT_SUBJECT_TYPE_SYSTEM = "SYSTEM";
    private static final String DEFAULT_SUBJECT_TYPE_ADMIN = "ADMIN";
    private static final String EVENT_STATUS_CREATED = "created";
    private static final String EVENT_STATUS_SKIPPED = "skipped";
    private static final String EVENT_STATUS_DUPLICATE = "duplicate";

    private static final String FILTER_KEY_USER_IDS = "userIds";
    private static final String FILTER_KEY_INCLUDE_USER_IDS = "includeUserIds";
    private static final String FILTER_KEY_EXCLUDE_USER_IDS = "excludeUserIds";
    private static final String FILTER_KEY_INCLUDE_ROLES = "includeRoles";
    private static final String FILTER_KEY_EXCLUDE_ROLES = "excludeRoles";
    private static final String FILTER_KEY_INCLUDE_SCOPES = "includeScopes";
    private static final String FILTER_KEY_EXCLUDE_SCOPES = "excludeScopes";
    private static final String FILTER_KEY_STATUS = "status";
    private static final String FILTER_KEY_STATUSES = "statuses";
    private static final String FILTER_KEY_VIP_LEVELS = "vipLevels";
    private static final String FILTER_KEY_VIP_ROLES = "vipRoles";
    private static final String FILTER_KEY_EMAIL_SUFFIX = "emailSuffix";
    private static final String FILTER_KEY_EMAIL_SUFFIXES = "emailSuffixes";
    private static final String FILTER_KEY_EMAIL_PREFIX = "emailPrefix";
    private static final String FILTER_KEY_EMAIL_PREFIXES = "emailPrefixes";
    private static final String FILTER_KEY_ASSET = "asset";
    private static final String FILTER_KEY_ASSETS = "assets";
    private static final String FILTER_KEY_ASSET_SYMBOL = "assetSymbol";
    private static final String FILTER_KEY_MIN_BALANCE = "minBalance";
    private static final String FILTER_KEY_MIN_ASSET_BALANCE = "minAssetBalance";
    private static final String FILTER_KEY_BALANCE_THRESHOLD = "balanceThreshold";
    private static final String FILTER_KEY_INCLUDE_ZERO_BALANCE = "includeZero";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Map<String, Map<String, String>> TEMPLATE_TEXT = Map.of(
            "ORDER_CREATED", Map.of(
                    "title", "訂單已建立",
                    "summary", "您的 {symbol} 委託已建立",
                    "body", "訂單已建立，請檢視最新委託狀態。"
            ),
            "ORDER_CANCELLED", Map.of(
                    "title", "訂單已取消",
                    "summary", "您的 {symbol} 訂單已取消",
                    "body", "訂單取消完成，若有疑問請聯繫客服。"
            ),
            "ORDER_PARTIALLY_FILLED", Map.of(
                    "title", "訂單部分成交",
                    "summary", "您的 {symbol} 訂單已部分成交",
                    "body", "訂單已部分成交，請檢視成交明細。"
            ),
            "ORDER_FILLED", Map.of(
                    "title", "訂單已成交",
                    "summary", "您的 {symbol} 訂單已成交",
                    "body", "訂單已完成，請確認資產變化。"
            ),
            "DEPOSIT_SUCCESS", Map.of(
                    "title", "入金成功",
                    "summary", "{asset} 入金已完成",
                    "body", "入金資金已入帳，請至資產頁確認。"
            ),
            "WITHDRAW_REQUESTED", Map.of(
                    "title", "出金申請已提交",
                    "summary", "您的出金申請已送出",
                    "body", "出金申請進入審核流程，請關注鏈上狀態。"
            ),
            "WITHDRAW_SUCCESS", Map.of(
                    "title", "出金完成",
                    "summary", "您的出金申請已完成",
                    "body", "出金資金已送出，請確認到帳狀態。"
            ),
            "WITHDRAW_FAILED", Map.of(
                    "title", "出金失敗",
                    "summary", "您的出金申請未通過",
                    "body", "請檢查失敗原因並重新提交。"
            ),
            "NEW_DEVICE_LOGIN", Map.of(
                    "title", "新裝置登入",
                    "summary", "您的帳戶偵測到新裝置登入",
                    "body", "請盡快確認是否為本人操作。"
            ),
            "PASSWORD_CHANGED", Map.of(
                    "title", "密碼已修改",
                    "summary", "帳戶登入密碼已更新",
                    "body", "若非本人操作，請立即啟動安全防護。"
            ),
            "KYC_APPROVED", Map.of(
                    "title", "KYC 已通過",
                    "summary", "您的帳戶 KYC 已通過",
                    "body", "法規審核完成，相關服務已啟用。"
            ),
            "KYC_REJECTED", Map.of(
                    "title", "KYC 未通過",
                    "summary", "KYC 審核未通過",
                    "body", "請補件後重新提交。"
            ),
            "API_KEY_CREATED", Map.of(
                    "title", "API Key 建立",
                    "summary", "新的 API Key 已建立",
                    "body", "請在安全設備中確認 API Key 使用情況。"
            ),
            "API_KEY_DELETED", Map.of(
                    "title", "API Key 刪除",
                    "summary", "API Key 已刪除",
                    "body", "該 API Key 已失效，請移除外部引用。"
            )
    );

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MessageCenterMessageJpaRepository messageRepository;
    private final MessageCenterMessageUserStateJpaRepository userStateRepository;
    private final MessageCenterNotificationPreferenceJpaRepository preferenceRepository;
    private final MessageCenterAnnouncementJpaRepository announcementRepository;
    private final AppUserRecordJpaRepository userRepository;
    private final WalletLedgerJournal walletLedgerJournal;
    private final PushGatewayService pushGatewayService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MessageCenterService(
            MessageCenterMessageJpaRepository messageRepository,
            MessageCenterMessageUserStateJpaRepository userStateRepository,
            MessageCenterNotificationPreferenceJpaRepository preferenceRepository,
            MessageCenterAnnouncementJpaRepository announcementRepository,
            AppUserRecordJpaRepository userRepository,
        WalletLedgerJournal walletLedgerJournal,
        PushGatewayService pushGatewayService,
        ObjectMapper objectMapper
    ) {
        this(messageRepository, userStateRepository, preferenceRepository, announcementRepository, userRepository,
                walletLedgerJournal,
                pushGatewayService, objectMapper, Clock.systemUTC());
    }

    MessageCenterService(
            MessageCenterMessageJpaRepository messageRepository,
            MessageCenterMessageUserStateJpaRepository userStateRepository,
            MessageCenterNotificationPreferenceJpaRepository preferenceRepository,
            MessageCenterAnnouncementJpaRepository announcementRepository,
            AppUserRecordJpaRepository userRepository,
            WalletLedgerJournal walletLedgerJournal,
            PushGatewayService pushGatewayService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.messageRepository = messageRepository;
        this.userStateRepository = userStateRepository;
        this.preferenceRepository = preferenceRepository;
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.walletLedgerJournal = walletLedgerJournal;
        this.pushGatewayService = pushGatewayService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MessageCenterUserDtos.MessageListResponse listMessages(
            long uid,
            List<String> category,
            String search,
            String status,
            boolean archived,
            String cursor,
            Integer limit,
            boolean pinnedFirst,
            boolean excludeDeleted
    ) {
        int pageSize = sanitizePageSize(limit);
        MessageCursor parsedCursor = decodeCursor(cursor);

        List<String> categories = parseCategoryFilters(category);
        List<String> categoryParam = categories.isEmpty() ? null : categories;

        String normalizedSearch = normalizeSearch(search);
        boolean unreadOnly = isUnreadStatus(status);

        List<MessageCenterListProjection> rows = userStateRepository.findMessagesByUser(
                uid,
                unreadOnly,
                archived,
                categoryParam,
                normalizedSearch,
                parsedCursor == null ? null : parsedCursor.createdAt(),
                parsedCursor == null ? null : parsedCursor.messageId(),
                excludeDeleted,
                pinnedFirst,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }

        Instant now = Instant.now(clock);
        List<MessageCenterUserDtos.MessageListItem> items = rows.stream()
                .map(item -> toListItem(item, now))
                .toList();

        String nextCursor = hasMore && !rows.isEmpty()
                ? encodeCursor(rows.get(rows.size() - 1))
                : null;

        return new MessageCenterUserDtos.MessageListResponse(items, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public MessageCenterUserDtos.MessageAnnouncementListResponse listAnnouncements(
            String status,
            String category,
            Instant from,
            Instant to,
            String cursor,
            Integer limit
    ) {
        // Admin-only announcement list is cursor-based and reuses createdAt/id ordering.
        int pageSize = sanitizePageSize(limit);
        MessageCursor parsedCursor = decodeCursor(cursor);

        String statusValue = status == null ? null : MessageAnnouncementStatus.parse(status).name();
        String categoryValue = category == null ? null : parseCategory(category).name();
        String cursorMessageId = parsedCursor == null ? null : parsedCursor.messageId();

        List<MessageCenterAnnouncement> rows = announcementRepository.findAnnouncements(
                statusValue,
                categoryValue,
                from,
                to,
                parsedCursor == null ? null : parsedCursor.createdAt(),
                cursorMessageId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }

        List<MessageCenterUserDtos.MessageAnnouncementListItemResponse> items = rows.stream()
                .map(item -> new MessageCenterUserDtos.MessageAnnouncementListItemResponse(
                        item.getId(),
                        item.getTitle(),
                        item.getSummary(),
                        item.getCategory(),
                        item.getSeverity(),
                        item.getStatus(),
                        item.getDeliveryMode(),
                        item.getSendAt(),
                        item.getExpireAt(),
                        item.getCreatedAt(),
                        item.getAudienceType(),
                        item.getEstimatedRecipients()
                ))
                .toList();

        String nextCursor = hasMore && !rows.isEmpty()
                ? encodeCursor(rows.get(rows.size() - 1).getCreatedAt(), rows.get(rows.size() - 1).getId())
                : null;

        return new MessageCenterUserDtos.MessageAnnouncementListResponse(items, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public MessageCenterUserDtos.MessageAnnouncementDetailResponse getAnnouncementDetail(String announcementId) {
        // Admin-only detail response must carry full metadata + delivery statistics.
        MessageCenterAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ANNOUNCEMENT_NOT_FOUND));

        return new MessageCenterUserDtos.MessageAnnouncementDetailResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getSummary(),
                announcement.getCategory(),
                announcement.getSeverity(),
                announcement.getTemplateCode(),
                parseJsonMap(announcement.getTemplateVarsJson()),
                announcement.getAudienceType(),
                parseJsonMap(announcement.getAudienceData()),
                announcement.getStatus(),
                announcement.getDeliveryMode(),
                announcement.getDedupeKey(),
                announcement.getSendAt(),
                announcement.getExpireAt(),
                announcement.getCreatedAt(),
                announcement.getCreatedBy(),
                announcement.getUpdatedAt(),
                new MessageCenterUserDtos.MessageAnnouncementDeliveryStats(
                        announcement.getSentCount(),
                        announcement.getFailedCount(),
                        announcement.getSkippedCount()
                )
        );
    }

    @Transactional(readOnly = true)
    public MessageCenterUserDtos.MessageDetailResponse getMessageDetail(long uid, String messageId, boolean autoRead) {
        MessageCenterMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND));

        MessageCenterMessageUserState state = requireActiveState(uid, messageId);

        if (autoRead) {
            markRead(uid, messageId);
            state = userStateRepository.findByUidAndMessageId(uid, messageId)
                    .orElse(state);
        }

        Map<String, Object> metadata = parseJsonMap(message.getMetadataJson());

        return new MessageCenterUserDtos.MessageDetailResponse(
                message.getId(),
                message.getTitle(),
                message.getSummary(),
                message.getBody(),
                message.getCategory(),
                message.getSeverity(),
                message.getCreatedAt(),
                message.getEffectiveAt(),
                message.getExpireAt(),
                state.isRead(),
                state.getReadAt(),
                state.isDeleted(),
                state.isArchived(),
                state.isPinned(),
                isExpired(state, message, Instant.now(clock)),
                message.isScheduled(),
                message.getActionUrl(),
                message.getActionLabel(),
                metadata
        );
    }

    @Transactional
    public MessageCenterUserDtos.MessageActionResponse markRead(long uid, String messageId) {
        requireActiveState(uid, messageId);
        userStateRepository.markRead(uid, messageId, Instant.now(clock));

        MessageCenterMessageUserState state = userStateRepository.findByUidAndMessageId(uid, messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND));

        return new MessageCenterUserDtos.MessageActionResponse(
                messageId,
                state.isRead(),
                state.getReadAt(),
                state.isDeleted(),
                state.isArchived(),
                state.isPinned()
        );
    }

    @Transactional
    public MessageCenterUserDtos.MessageActionResponse archive(long uid, String messageId) {
        requireActiveState(uid, messageId);
        userStateRepository.archive(uid, messageId, Instant.now(clock));

        MessageCenterMessageUserState state = userStateRepository.findByUidAndMessageId(uid, messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND));

        return stateResponse(messageId, state);
    }

    @Transactional
    public MessageCenterUserDtos.MessageActionResponse unarchive(long uid, String messageId) {
        requireActiveState(uid, messageId);
        userStateRepository.unarchive(uid, messageId, Instant.now(clock));

        MessageCenterMessageUserState state = userStateRepository.findByUidAndMessageId(uid, messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND));

        return stateResponse(messageId, state);
    }

    @Transactional
    public MessageCenterUserDtos.MessageActionResponse softDelete(long uid, String messageId) {
        requireActiveState(uid, messageId);
        userStateRepository.softDelete(uid, messageId, Instant.now(clock));

        MessageCenterMessageUserState state = userStateRepository.findByUidAndMessageId(uid, messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND));

        return stateResponse(messageId, state);
    }

    @Transactional
    public MessageCenterUserDtos.MessageActionResponse pin(long uid, String messageId, boolean pinned) {
        MessageCenterMessageUserState state = requireActiveState(uid, messageId);

        if (pinned) {
            userStateRepository.pin(uid, messageId, Instant.now(clock), Instant.now(clock));
        } else {
            userStateRepository.unpin(uid, messageId, Instant.now(clock));
        }

        MessageCenterMessageUserState updated = userStateRepository.findByUidAndMessageId(uid, messageId)
                .orElse(state);

        return stateResponse(messageId, updated);
    }

    @Transactional
    public MessageCenterUserDtos.MessageReadAllResponse markAllRead(long uid, String scope, String category) {
        String normalizedScope = scope == null ? "" : scope.trim().toUpperCase(Locale.ROOT);
        boolean categoryScope = "CATEGORY".equals(normalizedScope);
        boolean allScope = normalizedScope.isBlank() || "ALL".equals(normalizedScope);
        if (!allScope && !categoryScope) {
            throw new IllegalArgumentException("scope must be ALL or CATEGORY");
        }

        if (categoryScope && (category == null || category.isBlank())) {
            throw new IllegalArgumentException("category is required when scope is CATEGORY");
        }

        boolean archived = false;
        List<String> categories = parseCategoryFilters(List.ofNullable(category));
        if (categoryScope && categories.isEmpty()) {
            throw new IllegalArgumentException("invalid category");
        }

        List<MessageCenterMessageUserState> unreadStates = userStateRepository.findUnreadForReadAll(
                uid,
                archived,
                categories.isEmpty() ? null : categories
        );

        int now = 0;
        Instant readAt = Instant.now(clock);
        for (MessageCenterMessageUserState state : unreadStates) {
            now += userStateRepository.markRead(uid, state.getMessageId(), readAt);
        }
        return new MessageCenterUserDtos.MessageReadAllResponse(now);
    }

    @Transactional(readOnly = true)
    public MessageCenterUserDtos.MessageUnreadCountResponse getUnreadCount(long uid, boolean excludeArchived) {
        long unread = userStateRepository.countUnread(uid, excludeArchived);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (MessageCategory category : MessageCategory.values()) {
            byCategory.put(category.name(), 0L);
        }

        for (MessageCenterUnreadCountProjection projection : userStateRepository.countUnreadByCategory(uid, excludeArchived)) {
            byCategory.put(projection.getCategory(), projection.getUnreadCount());
        }

        return new MessageCenterUserDtos.MessageUnreadCountResponse(unread, byCategory);
    }

    @Transactional(readOnly = true)
    public MessageCenterUserDtos.MessagePreferencesResponse getPreferences(long uid) {
        List<MessageCenterNotificationPreference> saved = preferenceRepository.findByUid(uid);
        Map<String, MessageCenterNotificationPreference> preferenceByCategory = saved.stream()
                .collect(Collectors.toMap(
                        MessageCenterNotificationPreference::getCategory,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<MessageCenterUserDtos.MessagePreferenceItem> items = new ArrayList<>();
        for (MessageCategory category : MessageCategory.values()) {
            MessageCenterNotificationPreference preference = preferenceByCategory.get(category.name());
            boolean inApp = preference == null || preference.isInAppEnabled();
            boolean email = preference == null || preference.isEmailEnabled();
            boolean sms = preference == null || preference.isSmsEnabled();
            boolean push = preference == null || preference.isPushEnabled();
            MessageCenterNotificationPreference fallback = preference == null ? new MessageCenterNotificationPreference() : preference;
            if (fallback.getCategory() == null) {
                fallback.setUid(uid);
                fallback.setCategory(category.name());
            }

            items.add(new MessageCenterUserDtos.MessagePreferenceItem(
                    category.name(),
                    inApp,
                    email,
                    sms,
                    push,
                    fallback.lockedChannels()
            ));
        }

        return new MessageCenterUserDtos.MessagePreferencesResponse(items);
    }

    @Transactional
    public MessageCenterUserDtos.MessagePreferenceUpdateResponse updatePreferences(
            long uid,
            String actor,
            MessageCenterUserDtos.MessagePreferenceUpdateRequest request
    ) {
        Map<String, MessageCenterUserDtos.MessagePreferenceItem> requested =
                request == null || request.preferences() == null
                        ? Map.of()
                        : request.preferences().stream()
                        .filter(item -> item != null && item.category() != null)
                        .collect(Collectors.toMap(
                                item -> normalizeCategory(item.category()),
                                item -> item,
                                (left, right) -> right,
                                LinkedHashMap::new
                        ));

        Set<String> seen = new LinkedHashSet<>();
        Map<String, MessageCenterNotificationPreference> current = preferenceRepository.findByUid(uid).stream()
                .collect(Collectors.toMap(
                        MessageCenterNotificationPreference::getCategory,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        int updated = 0;
        for (Map.Entry<String, MessageCenterUserDtos.MessagePreferenceItem> entry : requested.entrySet()) {
            MessageCategory category = parseCategory(entry.getKey());
            String categoryName = category.name();
            if (!seen.add(categoryName)) {
                throw new BusinessException(BusinessErrorCode.INVALID_CATEGORY);
            }

            MessageCenterUserDtos.MessagePreferenceItem item = entry.getValue();
            MessageCenterNotificationPreference preference = current.get(categoryName);
            if (preference == null) {
                preference = new MessageCenterNotificationPreference();
                preference.setUid(uid);
                preference.setCategory(categoryName);
            }

            boolean locked = category == MessageCategory.SECURITY || category == MessageCategory.COMPLIANCE;
            boolean requestedEmail = item == null || item.emailEnabled();
            boolean requestedSms = item != null && item.smsEnabled();
            boolean requestedPush = item != null && item.pushEnabled();

            if (locked && (!requestedEmail || !requestedSms || !requestedPush)) {
                throw new BusinessException(BusinessErrorCode.PREFERENCE_LOCKED);
            }

            boolean oldEmail = preference.isEmailEnabled();
            boolean oldSms = preference.isSmsEnabled();
            boolean oldPush = preference.isPushEnabled();

            preference.setInAppEnabled(true);
            preference.setEmailEnabled(requestedEmail);
            preference.setSmsEnabled(requestedSms);
            preference.setPushEnabled(requestedPush);
            preference.setUpdatedBy(actor == null ? DEFAULT_SUBJECT : actor);

            if (oldEmail != preference.isEmailEnabled() || oldSms != preference.isSmsEnabled() || oldPush != preference.isPushEnabled()) {
                updated++;
            }

            preferenceRepository.save(preference);
        }

        return new MessageCenterUserDtos.MessagePreferenceUpdateResponse(
                updated,
                getPreferences(uid).preferences()
        );
    }

    @Transactional
    public MessageCenterUserDtos.MessageSendOutcome sendSystemMessage(
            MessageCenterUserDtos.MessageSystemSendRequest request,
            String actor,
            String actorType
    ) {
        if (request == null) {
            throw new IllegalArgumentException("system message request is required");
        }

        MessageCategory category = parseCategory(request.category());
        MessageSeverity severity = MessageSeverity.parse(request.severity());
        List<Long> recipients = resolveRecipients(
                request.audienceType(),
                request.recipientUserIds(),
                request.audienceData()
        );

        if (recipients.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }

        String templateCode = request.templateCode();
        Map<String, Object> templateVars = mapOrEmpty(request.templateVars());
        Map<String, Object> metadata = mapOrEmpty(request.metadata());

        String title = resolveTemplateText(templateCode, "title", request.title(), templateVars, request.category());
        String summary = resolveTemplateText(templateCode, "summary", request.summary(), templateVars, request.category());
        String body = resolveTemplateText(templateCode, "body", request.body(), templateVars, request.category());

        Instant now = Instant.now(clock);
        Instant sendAt = request.sendAt() == null ? now : request.sendAt();
        Instant effectiveAt = request.effectiveAt() == null ? now : request.effectiveAt();

        MessageCreationResult messageResult = upsertMessage(
                request.dedupeKey(),
                title,
                summary,
                body,
                category,
                severity,
                templateCode,
                templateVars,
                metadata,
                null,
                actorType == null ? DEFAULT_SUBJECT_TYPE_SYSTEM : actorType,
                actor == null ? DEFAULT_SUBJECT : actor,
                request.sourceEventType(),
                request.sourceEventId(),
                request.sourceEventHash(),
                request.actionUrl(),
                request.actionLabel(),
                sendAt,
                effectiveAt,
                request.expireAt()
        );

        MessageDispatchStats stats = sendToRecipients(messageResult.message(), recipients, Instant.now(clock));

        return new MessageCenterUserDtos.MessageSendOutcome(
                messageResult.message().getId(),
                messageResult.message().getTitle(),
                messageResult.message().getCategory(),
                messageResult.message().getSeverity(),
                stats.sentTo(),
                stats.skippedTo(),
                messageResult.existed(),
                sendAt
        );
    }

    @Transactional
    public MessageCenterUserDtos.MessageSystemEventResponse sendSystemEvent(
            MessageCenterUserDtos.MessageSystemEventRequest request,
            String actor,
            String actorType
    ) {
        // System event API is eventId/userId 去重，不可重複建立相同事件通知。
        if (request == null) {
            throw new IllegalArgumentException("system event request is required");
        }
        if (request.eventType() == null || request.eventType().isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (request.sourceUserId() <= 0) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }

        MessageCategory category = parseCategory(request.category());
        MessageSeverity severity = MessageSeverity.parse(request.severity());
        long targetUid = request.sourceUserId();
        Instant now = Instant.now(clock);
        Instant sendAt = request.eventTimestamp() == null ? now : request.eventTimestamp();
        Instant effectiveAt = sendAt;

        String dedupeKey = resolveEventDedupeKey(request.eventType(), request.eventId(), request.sourceUserId(), request.dedupeKey());
        Map<String, Object> templateVars = new LinkedHashMap<>();
        templateVars.put("eventType", request.eventType());
        templateVars.put("eventId", request.eventId());
        templateVars.put("sourceUserId", request.sourceUserId());
        templateVars.put("eventTimestamp", sendAt);
        templateVars.putAll(mapOrEmpty(request.templateVars()));

        Map<String, Object> metadata = mapOrEmpty(request.metadataOverrides());
        String templateCode = request.templateCode();

        String title = resolveTemplateText(templateCode, "title", null, templateVars, category.name());
        String summary = resolveTemplateText(templateCode, "summary", null, templateVars, category.name());
        String body = resolveTemplateText(templateCode, "body", null, templateVars, category.name());

        MessageCreationResult messageResult = upsertMessage(
                dedupeKey,
                title,
                summary,
                body,
                category,
                severity,
                templateCode,
                templateVars,
                metadata,
                targetUid,
                actorType == null ? DEFAULT_SUBJECT_TYPE_SYSTEM : actorType,
                actor == null ? DEFAULT_SUBJECT : actor,
                request.eventType(),
                request.eventId(),
                dedupeKey,
                request.actionUrl(),
                request.actionLabel(),
                sendAt,
                effectiveAt,
                null
        );

        if (userStateRepository.existsByUidAndDedupeKey(targetUid, dedupeKey)) {
            return new MessageCenterUserDtos.MessageSystemEventResponse(
                    messageResult.message().getId(),
                    request.sourceUserId(),
                    EVENT_STATUS_DUPLICATE
            );
        }

        MessageDispatchStats stats = sendToRecipients(messageResult.message(), List.of(targetUid), now);
        String status = stats.sentTo() > 0 ? EVENT_STATUS_CREATED : EVENT_STATUS_SKIPPED;

        return new MessageCenterUserDtos.MessageSystemEventResponse(
                messageResult.message().getId(),
                request.sourceUserId(),
                messageResult.existed() && EVENT_STATUS_SKIPPED.equals(status)
                        ? EVENT_STATUS_DUPLICATE
                        : status
        );
    }

    @Transactional
    public MessageCenterUserDtos.MessageSystemEventBatchResponse sendSystemEventBatch(
            MessageCenterUserDtos.MessageSystemEventBatchRequest request,
            String actor,
            String actorType
    ) {
        List<MessageCenterUserDtos.MessageSystemEventRequest> events = request == null || request.events() == null
                ? List.of()
                : request.events();
        List<MessageCenterUserDtos.MessageSystemEventResponse> results = new ArrayList<>(events.size());

        for (MessageCenterUserDtos.MessageSystemEventRequest event : events) {
            results.add(sendSystemEvent(event, actor, actorType));
        }

        return new MessageCenterUserDtos.MessageSystemEventBatchResponse(results);
    }

    @Transactional
    public MessageCenterUserDtos.MessageAnnouncementCreateResponse createAnnouncement(
            MessageCenterUserDtos.MessageAnnouncementCreateRequest request,
            String actor
    ) {
        if (request == null) {
            throw new IllegalArgumentException("announcement request is required");
        }

        MessageCategory category = parseCategory(request.category());
        MessageSeverity severity = MessageSeverity.parse(request.severity());
        MessageDeliveryMode deliveryMode = MessageDeliveryMode.parse(request.deliveryMode());
        MessageAudienceType audienceType = MessageAudienceType.parse(request.audienceType());

        List<Long> recipientIds = resolveRecipients(audienceType.name(), request.recipientUserIds(), request.audienceData());
        Instant now = Instant.now(clock);
        Instant sendAt = request.sendAt() == null ? now : request.sendAt();

        Map<String, Object> templateVars = mapOrEmpty(request.templateVars());
        MessageTemplateSnapshot template = resolveDefaultTemplate(request.templateCode(), templateVars, request.summary());

        MessageCenterAnnouncement announcement = new MessageCenterAnnouncement();
        announcement.setId(UUID.randomUUID().toString());
        announcement.setTitle(summaryOrDefault(request.title(), template.title()));
        announcement.setSummary(summaryOrDefault(request.summary(), template.summary()));
        announcement.setCategory(category.name());
        announcement.setSeverity(severity.name());
        announcement.setTemplateCode(request.templateCode());
        announcement.setTemplateVarsJson(toJson(templateVars));
        announcement.setActionUrl(request.actionUrl());
        announcement.setActionLabel(request.actionLabel());
        announcement.setAudienceType(audienceType.name());
        announcement.setAudienceData(toJson(createAudienceData(recipientIds, audienceType, request.audienceData())));
        announcement.setStatus((sendAt.isAfter(now) ? MessageAnnouncementStatus.SCHEDULED : MessageAnnouncementStatus.PUBLISHED).name());
        announcement.setDeliveryMode(deliveryMode.name());
        announcement.setDedupeKey(blankOr(request.dedupeKey()));
        announcement.setEstimatedRecipients(Math.max(recipientIds.size(), 0));
        announcement.setSentCount(0L);
        announcement.setFailedCount(0L);
        announcement.setSkippedCount(0L);
        announcement.setCreatedBy(actor == null ? DEFAULT_SUBJECT : actor);
        announcement.setCreatedByType(actor == null ? DEFAULT_SUBJECT_TYPE_SYSTEM : DEFAULT_SUBJECT_TYPE_ADMIN);
        announcement.setSendAt(sendAt);
        announcement.setExpireAt(request.expireAt());

        announcementRepository.save(announcement);

        MessageCenterUserDtos.MessageSendOutcome outcome = null;
        if (!sendAt.isAfter(now)) {
            MessageCenterUserDtos.MessageSendOutcome publishResult = publishAnnouncementNow(announcement, template, templateVars, request.dedupeKey());
            announcement.setStatus(MessageAnnouncementStatus.PUBLISHED.name());
            announcement.setSentCount(publishResult.sentTo());
            announcement.setSkippedCount(publishResult.skippedTo());
            announcementRepository.save(announcement);
            outcome = publishResult;
        }

        return new MessageCenterUserDtos.MessageAnnouncementCreateResponse(
                announcement.getId(),
                announcement.getStatus(),
                announcement.getEstimatedRecipients(),
                outcome
        );
    }

    @Transactional
    public MessageCenterUserDtos.MessageAnnouncementCancelResponse cancelScheduledAnnouncement(String announcementId) {
        MessageCenterAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ANNOUNCEMENT_NOT_FOUND));

        if (!MessageAnnouncementStatus.SCHEDULED.name().equals(announcement.getStatus())) {
            throw new BusinessException(BusinessErrorCode.SCHEDULE_NOT_CANCELABLE);
        }

        announcement.setStatus(MessageAnnouncementStatus.CANCELLED.name());
        return new MessageCenterUserDtos.MessageAnnouncementCancelResponse(announcementId, true);
    }

    @Transactional
    public int publishDueAnnouncements(int batchSize) {
        Instant now = Instant.now(clock);
        int safeBatch = Math.max(1, Math.min(batchSize <= 0 ? 1 : batchSize, MAX_PAGE_LIMIT));

        List<String> ids = announcementRepository.findReadyScheduled(
                        MessageAnnouncementStatus.SCHEDULED.name(),
                        now,
                        PageRequest.of(0, safeBatch, Sort.by(Sort.Direction.ASC, "sendAt", "id"))
                )
                .stream()
                .map(MessageCenterAnnouncement::getId)
                .toList();

        int processed = 0;
        for (String id : ids) {
            if (publishScheduledAnnouncement(id)) {
                processed++;
            }
        }

        return processed;
    }

    @Transactional
    public boolean publishScheduledAnnouncement(String announcementId) {
        MessageCenterAnnouncement locked = announcementRepository.lockByIdForUpdate(announcementId)
                .orElse(null);
        if (locked == null || !MessageAnnouncementStatus.SCHEDULED.name().equals(locked.getStatus())) {
            return false;
        }

        Instant now = Instant.now(clock);
        if (locked.getSendAt() == null || locked.getSendAt().isAfter(now)) {
            return false;
        }

        List<Long> recipients = resolveRecipients(
                locked.getAudienceType(),
                parseAudienceUserIds(parseJsonMap(locked.getAudienceData())),
                parseJsonMap(locked.getAudienceData())
        );

        MessageTemplateSnapshot template = resolveDefaultTemplate(locked.getTemplateCode(), parseJsonMap(locked.getTemplateVarsJson()), locked.getSummary());
        MessageCreationResult messageResult = upsertMessage(
                locked.getDedupeKey(),
                locked.getTitle(),
                locked.getSummary(),
                template.body(),
                MessageCategory.parse(locked.getCategory()),
                MessageSeverity.parse(locked.getSeverity()),
                locked.getTemplateCode(),
                parseJsonMap(locked.getTemplateVarsJson()),
                Map.of(),
                null,
                locked.getCreatedByType(),
                locked.getCreatedBy(),
                null,
                null,
                null,
                locked.getActionUrl(),
                locked.getActionLabel(),
                locked.getSendAt() == null ? now : locked.getSendAt(),
                now,
                locked.getExpireAt()
        );

        MessageDispatchStats stats = sendToRecipients(messageResult.message(), recipients, now);
        locked.setStatus(MessageAnnouncementStatus.PUBLISHED.name());
        locked.setSentCount(stats.sentTo());
        locked.setSkippedCount(stats.skippedTo());
        announcementRepository.save(locked);

        return true;
    }

    private MessageCreationResult upsertMessage(
            String dedupeKey,
            String title,
            String summary,
            String body,
            MessageCategory category,
            MessageSeverity severity,
            String templateCode,
            Map<String, Object> templateVars,
            Map<String, Object> metadata,
            Long sourceUserId,
            String createdByType,
            String createdBy,
            String sourceEventType,
            String sourceEventId,
            String sourceEventHash,
            String actionUrl,
            String actionLabel,
            Instant scheduledAt,
            Instant effectiveAt,
            Instant expireAt
    ) {
        if (dedupeKey != null && !dedupeKey.isBlank()) {
            Optional<MessageCenterMessage> existing = messageRepository.findFirstByDedupeKey(dedupeKey.trim());
            if (existing.isPresent()) {
                return new MessageCreationResult(existing.get(), true);
            }
        }

        Instant now = Instant.now(clock);
        Instant effective = effectiveAt == null ? now : effectiveAt;
        Instant planned = scheduledAt == null ? now : scheduledAt;

        MessageCenterMessage message = new MessageCenterMessage();
        message.setId(UUID.randomUUID().toString());
        message.setTemplateCode(blankOr(templateCode));
        message.setTitle(title == null ? "" : title.trim());
        message.setSummary(summary == null ? "" : summary.trim());
        message.setBody(body == null ? "" : body.trim());
        message.setCategory(category.name());
        message.setSeverity(severity.name());
        message.setActionUrl(blankOr(actionUrl));
        message.setActionLabel(blankOr(actionLabel));
        message.setMetadataJson(toJson(metadata));
        message.setTemplateVarsJson(toJson(templateVars));
        message.setSourceUserId(sourceUserId);
        message.setSourceEventType(sourceEventType);
        message.setSourceEventId(sourceEventId);
        message.setSourceEventHash(sourceEventHash);
        message.setDedupeKey(blankOr(dedupeKey));
        message.setCreatedBy(createdBy == null ? DEFAULT_SUBJECT : createdBy);
        message.setCreatedByType(createdByType == null || createdByType.isBlank() ? DEFAULT_SUBJECT_TYPE_SYSTEM : createdByType);
        message.setEffectiveAt(effective);
        message.setExpireAt(expireAt);
        message.setScheduled(planned.isAfter(now));
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        return new MessageCreationResult(messageRepository.save(message), false);
    }

    private MessageDispatchStats sendToRecipients(MessageCenterMessage message, List<Long> recipients, Instant now) {
        long sent = 0;
        long skipped = 0;

        for (long uid : recipients) {
            if (uid <= 0L) {
                skipped++;
                continue;
            }

            MessageCenterNotificationPreference inAppPreference = preferenceRepository
                    .findByUidAndCategory(uid, message.getCategory())
                    .orElse(null);
            if (inAppPreference != null && !inAppPreference.isInAppEnabled()) {
                skipped++;
                continue;
            }

            if (userStateRepository.existsByUidAndMessageId(uid, message.getId())
                    || (!blank(message.getDedupeKey()) && userStateRepository.existsByUidAndDedupeKey(uid, message.getDedupeKey()))) {
                skipped++;
                continue;
            }

            MessageCenterMessageUserState state = new MessageCenterMessageUserState();
            state.setUid(uid);
            state.setMessageId(message.getId());
            state.setRead(false);
            state.setDeleted(false);
            state.setArchived(false);
            state.setPinned(false);
            state.setDedupeKey(message.getDedupeKey());
            state.setCreatedAt(now);
            state.setUpdatedAt(now);
            userStateRepository.save(state);

            pushGatewayService.publishUser(uid, "message.new", messageNewEvent(message));
            sendUnreadCountIfAvailable(uid);
            sent++;
        }

        return new MessageDispatchStats(sent, skipped);
    }

    private void sendUnreadCountIfAvailable(long uid) {
        Long unread;
        Map<String, Long> byCategory = new java.util.LinkedHashMap<>();
        try {
            unread = userStateRepository.countUnread(uid, true);
            Map<String, Long> current = userStateRepository.countUnreadByCategory(uid, true).stream()
                    .collect(Collectors.toMap(
                            MessageCenterUnreadCountProjection::getCategory,
                            MessageCenterUnreadCountProjection::getUnreadCount,
                            (left, right) -> right,
                            java.util.HashMap::new
                    ));
            if (unread != null) {
                byCategory.putAll(current);
                for (MessageCategory category : MessageCategory.values()) {
                    byCategory.putIfAbsent(category.name(), 0L);
                }
            }
        } catch (RuntimeException ex) {
            unread = null;
            return;
        }
        if (unread == null) {
            return;
        }
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("unreadCount", unread);
        payload.put("byCategory", byCategory);
        pushGatewayService.publishUser(uid, "message.unreadCount", payload);
    }

    private MessageCenterUserDtos.MessageActionResponse stateResponse(String messageId, MessageCenterMessageUserState state) {
        return new MessageCenterUserDtos.MessageActionResponse(
                messageId,
                state.isRead(),
                state.getReadAt(),
                state.isDeleted(),
                state.isArchived(),
                state.isPinned()
        );
    }

    private MessageCenterMessageUserState requireActiveState(long uid, String messageId) {
        MessageCenterMessageUserState state = userStateRepository.findByUidAndMessageId(uid, messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND));
        if (state.isDeleted()) {
            throw new BusinessException(BusinessErrorCode.MESSAGE_NOT_FOUND);
        }
        return state;
    }

    private static boolean isUnreadStatus(String status) {
        return "UNREAD".equalsIgnoreCase(status);
    }

    private int sanitizePageSize(Integer raw) {
        if (raw == null || raw < 1) {
            return DEFAULT_PAGE_LIMIT;
        }
        return Math.min(raw, MAX_PAGE_LIMIT);
    }

    private static String normalizeCategory(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static MessageCategory parseCategory(String value) {
        return MessageCategory.parse(normalizeCategory(value));
    }

    private List<String> parseCategoryFilters(List<String> rawCategories) {
        if (rawCategories == null || rawCategories.isEmpty()) {
            return List.of();
        }

        return rawCategories.stream()
                .flatMap(raw -> List.of((raw == null ? "" : raw).split(","))
                        .stream())
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseCategoryFromInput)
                .distinct()
                .collect(Collectors.toList());
    }

    private String parseCategoryFromInput(String value) {
        try {
            return parseCategory(value).name();
        } catch (RuntimeException ex) {
            throw new BusinessException(BusinessErrorCode.INVALID_CATEGORY);
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
    }

    private static boolean isExpired(MessageCenterMessageUserState state, MessageCenterMessage message, Instant now) {
        return state.isArchived() || isExpired(message, now);
    }

    private static boolean isExpired(Instant expireAt, Instant now) {
        return expireAt != null && !expireAt.isAfter(now);
    }

    private static boolean isExpired(MessageCenterMessage message, Instant now) {
        return isExpired(message.getExpireAt(), now);
    }

    private static class MessageDispatchStats {
        private final long sentTo;
        private final long skippedTo;

        MessageDispatchStats(long sentTo, long skippedTo) {
            this.sentTo = sentTo;
            this.skippedTo = skippedTo;
        }

        long sentTo() {
            return sentTo;
        }

        long skippedTo() {
            return skippedTo;
        }
    }

    private MessageCenterUserDtos.MessageListItem toListItem(MessageCenterListProjection projection, Instant now) {
        return new MessageCenterUserDtos.MessageListItem(
                projection.getMessageId(),
                projection.getTitle(),
                projection.getSummary(),
                projection.getCategory(),
                projection.getSeverity(),
                projection.getCreatedAt(),
                projection.getIsRead(),
                projection.getIsPinned(),
                projection.getIsDeleted(),
                projection.getIsArchived(),
                isExpired(projection.getExpireAt(), now),
                projection.getIsScheduled(),
                projection.getActionUrl(),
                projection.getActionLabel()
        );
    }

    private record MessageCursor(Instant createdAt, String messageId) {
    }

    private MessageCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int index = raw.indexOf(CURSOR_SEPARATOR);
            if (index <= 0 || index >= raw.length() - 1) {
                throw new IllegalArgumentException("bad cursor format");
            }

            long millis = Long.parseLong(raw.substring(0, index));
            String messageId = raw.substring(index + CURSOR_SEPARATOR.length());
            return new MessageCursor(Instant.ofEpochMilli(millis), messageId);
        } catch (RuntimeException ex) {
            throw new BusinessException(BusinessErrorCode.INVALID_CURSOR_FORMAT);
        }
    }

    private String encodeCursor(Instant createdAt, String messageId) {
        String raw = createdAt.toEpochMilli() + CURSOR_SEPARATOR + messageId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeCursor(MessageCenterListProjection projection) {
        String raw = projection.getCreatedAt().toEpochMilli() + CURSOR_SEPARATOR + projection.getMessageId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String blankOr(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private MessageTemplateSnapshot resolveDefaultTemplate(String templateCode, Map<String, Object> vars, String fallbackSummary) {
        String fallback = fallbackSummary == null || fallbackSummary.isBlank() ? "" : fallbackSummary;

        if (templateCode == null || templateCode.isBlank()) {
            return new MessageTemplateSnapshot(fallback, fallback, fallback);
        }

        Map<String, String> template = TEMPLATE_TEXT.get(templateCode.trim().toUpperCase(Locale.ROOT));
        if (template == null) {
            return new MessageTemplateSnapshot(templateCode.trim(), fallback, "");
        }

        return new MessageTemplateSnapshot(
                renderTemplate(template.get("title"), vars),
                renderTemplate(template.get("summary"), vars),
                renderTemplate(template.get("body"), vars)
        );
    }

    private String resolveTemplateText(
            String templateCode,
            String field,
            String explicit,
            Map<String, Object> vars,
            String fallbackCategory
    ) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }

        MessageTemplateSnapshot snapshot = resolveDefaultTemplate(templateCode, vars, fallbackCategory);
        return switch (field) {
            case "title" -> snapshot.title();
            case "summary" -> snapshot.summary();
            default -> snapshot.body();
        };
    }

    private String renderTemplate(String template, Map<String, Object> vars) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    private String summaryOrDefault(String summary, String fallback) {
        String value = summary == null ? "" : summary.trim();
        return value.isBlank() ? (fallback == null ? "" : fallback.trim()) : value;
    }

    private Map<String, Object> mapOrEmpty(Map<String, Object> source) {
        return source == null ? Collections.emptyMap() : source;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("json serialization failed", ex);
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("json parse failed", ex);
        }
    }

    private List<Long> resolveRecipients(String audienceTypeValue, List<Long> explicit, Map<String, Object> audienceData) {
        MessageAudienceType audienceType = MessageAudienceType.parse(audienceTypeValue);
        Map<String, Object> effectiveData = audienceData == null ? Map.of() : audienceData;
        return resolveRecipients(audienceType, explicit, effectiveData);
    }

    private List<Long> resolveRecipients(MessageAudienceType audienceType, List<Long> explicit, Map<String, Object> audienceData) {
        Map<String, Object> effectiveData = audienceData == null ? Map.of() : audienceData;
        return switch (audienceType) {
            case ALL -> userIdsInBatches();
            case USER_IDS -> {
                List<Long> resolved = new ArrayList<>();
                if (explicit != null) {
                    resolved.addAll(explicit);
                }
                if (resolved.isEmpty()) {
                    resolved.addAll(parseAudienceUserIds(effectiveData, FILTER_KEY_USER_IDS));
                }

                List<Long> filtered = resolved.stream()
                        .filter(uid -> uid != null && uid > 0)
                        .distinct()
                        .toList();

                if (filtered.isEmpty()) {
                    throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
                }
                yield filtered;
            }
            case VIP -> resolveVipRecipients(effectiveData);
            case HAS_ASSET -> resolveHasAssetRecipients(effectiveData);
            case CUSTOM_FILTER -> resolveCustomFilterRecipients(explicit, effectiveData);
        };
    }

    private List<Long> userIdsInBatches() {
        return resolveUsersByPredicate(user -> true, Set.of(), Set.of());
    }

    private List<Long> resolveVipRecipients(Map<String, Object> audienceData) {
        Set<String> vipRoleTokens = parseVipRoles(audienceData);
        if (vipRoleTokens.isEmpty()) {
            vipRoleTokens = Set.of("VIP");
        }

        Predicate<AppUserRecord> predicate = user -> {
            Set<String> roles = tokenize(user.getRoles());
            return hasAny(role -> vipRoleTokens.contains(role), roles);
        };

        List<Long> recipients = resolveUsersByPredicate(predicate, Set.of(), Set.of());
        if (recipients.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }
        return recipients;
    }

    private List<Long> resolveHasAssetRecipients(Map<String, Object> audienceData) {
        List<String> assets = parseAssetSymbols(audienceData);
        if (assets.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }

        BigDecimal minBalance = parseAudienceBalance(audienceData,
                FILTER_KEY_MIN_BALANCE,
                FILTER_KEY_BALANCE_THRESHOLD,
                FILTER_KEY_MIN_ASSET_BALANCE);
        boolean includeZeroBalance = parseAudienceBoolean(audienceData, FILTER_KEY_INCLUDE_ZERO_BALANCE);
        Set<Long> includeUsers = new LinkedHashSet<>(
                parseAudienceUserIds(audienceData, FILTER_KEY_INCLUDE_USER_IDS, FILTER_KEY_USER_IDS)
        );
        Set<Long> excludeUsers = new LinkedHashSet<>(
                parseAudienceUserIds(audienceData, FILTER_KEY_EXCLUDE_USER_IDS)
        );

        Predicate<AppUserRecord> predicate = user ->
                hasAnyAssetBalance(user.getId(), assets, minBalance == null ? ZERO : minBalance, includeZeroBalance);

        List<Long> recipients = resolveUsersByPredicate(
                predicate,
                includeUsers.isEmpty() ? null : includeUsers,
                excludeUsers
        );
        if (recipients.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }
        return recipients;
    }

    private List<Long> resolveCustomFilterRecipients(List<Long> explicit, Map<String, Object> audienceData) {
        Set<Long> includeUsers = explicit == null
                ? new LinkedHashSet<>()
                : explicit.stream()
                .filter(uid -> uid != null && uid > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        includeUsers.addAll(parseAudienceUserIds(audienceData, FILTER_KEY_INCLUDE_USER_IDS, FILTER_KEY_USER_IDS));
        Set<Long> excludeUsers = new LinkedHashSet<>(parseAudienceUserIds(audienceData, FILTER_KEY_EXCLUDE_USER_IDS));

        Set<String> includeRoles = parseTokenSet(audienceData, FILTER_KEY_INCLUDE_ROLES);
        Set<String> excludeRoles = parseTokenSet(audienceData, FILTER_KEY_EXCLUDE_ROLES);
        Set<String> includeScopes = parseTokenSet(audienceData, FILTER_KEY_INCLUDE_SCOPES);
        Set<String> excludeScopes = parseTokenSet(audienceData, FILTER_KEY_EXCLUDE_SCOPES);
        Set<String> statusFilters = parseTokenSet(audienceData, FILTER_KEY_STATUS, FILTER_KEY_STATUSES);
        Set<String> emailSuffixes = parseTokenSet(audienceData, FILTER_KEY_EMAIL_SUFFIX, FILTER_KEY_EMAIL_SUFFIXES);
        Set<String> emailPrefixes = parseTokenSet(audienceData, FILTER_KEY_EMAIL_PREFIX, FILTER_KEY_EMAIL_PREFIXES);
        Set<String> vipRoleTokens = parseVipRoles(audienceData);
        List<String> assetSymbols = parseAssetSymbols(audienceData);
        BigDecimal minBalance = parseAudienceBalance(audienceData,
                FILTER_KEY_MIN_BALANCE,
                FILTER_KEY_BALANCE_THRESHOLD,
                FILTER_KEY_MIN_ASSET_BALANCE);
        boolean includeZeroBalance = parseAudienceBoolean(audienceData, FILTER_KEY_INCLUDE_ZERO_BALANCE);

        Predicate<AppUserRecord> predicate = user -> {
            Set<String> userRoles = tokenize(user.getRoles());
            Set<String> userScopes = tokenize(user.getScopes());

            if (!statusFilters.isEmpty() && !statusFilters.contains(normalizeText(user.getStatus()))) {
                return false;
            }
            if (!includeRoles.isEmpty() && !hasAny(role -> includeRoles.contains(role), userRoles)) {
                return false;
            }
            if (hasAny(role -> excludeRoles.contains(role), userRoles)) {
                return false;
            }
            if (!includeScopes.isEmpty() && !hasAny(scope -> includeScopes.contains(scope), userScopes)) {
                return false;
            }
            if (hasAny(scope -> excludeScopes.contains(scope), userScopes)) {
                return false;
            }
            if (!emailPrefixes.isEmpty() && !matchesEmailPrefix(user.getEmail(), emailPrefixes)) {
                return false;
            }
            if (!emailSuffixes.isEmpty() && !matchesEmailSuffix(user.getEmail(), emailSuffixes)) {
                return false;
            }
            if (!vipRoleTokens.isEmpty() && !hasAny(role -> vipRoleTokens.contains(role), userRoles)) {
                return false;
            }
            if (!assetSymbols.isEmpty()
                    && !hasAnyAssetBalance(user.getId(), assetSymbols,
                    minBalance == null ? ZERO : minBalance, includeZeroBalance)) {
                return false;
            }
            return true;
        };

        List<Long> recipients = resolveUsersByPredicate(
                predicate,
                includeUsers.isEmpty() ? null : includeUsers,
                excludeUsers
        );
        if (recipients.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }
        return recipients;
    }

    private List<Long> resolveUsersByPredicate(
            Predicate<AppUserRecord> predicate,
            Set<Long> includeUsers,
            Set<Long> excludeUsers
    ) {
        List<Long> result = new ArrayList<>();
        int page = 0;

        while (true) {
            List<AppUserRecord> users = userRepository.findAll(PageRequest.of(page, BATCH_USER_SIZE, Sort.by("id")))
                    .getContent();
            for (AppUserRecord user : users) {
                Long uid = user == null ? null : user.getId();
                if (uid == null || uid <= 0L) {
                    continue;
                }
                if (excludeUsers != null && !excludeUsers.isEmpty() && excludeUsers.contains(uid)) {
                    continue;
                }
                if (includeUsers != null && !includeUsers.contains(uid)) {
                    continue;
                }
                if (predicate.test(user)) {
                    result.add(uid);
                }
            }

            if (users.size() < BATCH_USER_SIZE) {
                break;
            }
            page++;
        }

        return result;
    }

    private boolean hasAny(java.util.function.Predicate<String> matcher, Set<String> tokens) {
        for (String token : tokens) {
            if (matcher.test(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyAssetBalance(long uid, List<String> assets, BigDecimal minBalance, boolean includeZeroBalance) {
        if (assets == null || assets.isEmpty()) {
            return false;
        }

        BigDecimal threshold = minBalance == null ? ZERO : minBalance;
        for (String asset : assets) {
            BigDecimal balance = walletLedgerJournal.findLatestByUidAndAsset(uid, normalizeAsset(asset))
                    .map(entry -> entry.getBalanceAfter())
                    .orElse(ZERO);
            int compare = balance == null ? -1 : balance.compareTo(threshold);
            if ((includeZeroBalance && compare >= 0) || (!includeZeroBalance && compare > 0)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> parseVipRoles(Map<String, Object> audienceData) {
        Set<String> vipRoles = new LinkedHashSet<>(parseTokenSet(audienceData, FILTER_KEY_VIP_ROLES));
        Set<String> levels = parseTokenSet(audienceData, FILTER_KEY_VIP_LEVELS);

        for (String level : levels) {
            vipRoles.add("VIP" + level);
            vipRoles.add("VIP_" + level);
            vipRoles.add("VIP-" + level);
        }
        return vipRoles;
    }

    private boolean matchesEmailPrefix(String email, Set<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return true;
        }
        String normalizedEmail = normalizeText(email);
        if (normalizedEmail.isBlank()) {
            return false;
        }

        for (String prefix : prefixes) {
            if (normalizedEmail.startsWith(normalizeText(prefix))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesEmailSuffix(String email, Set<String> suffixes) {
        if (suffixes == null || suffixes.isEmpty()) {
            return true;
        }
        String normalizedEmail = normalizeText(email);
        if (normalizedEmail.isBlank()) {
            return false;
        }

        for (String suffix : suffixes) {
            String normalized = normalizeText(suffix);
            if (normalized.isBlank()) {
                continue;
            }
            String normalizedSuffix = normalized.startsWith("@") ? normalized : "@" + normalized;
            if (normalizedEmail.endsWith(normalizedSuffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean parseAudienceBoolean(Map<String, Object> audienceData, String... keys) {
        for (String key : keys) {
            if (!audienceData.containsKey(key)) {
                continue;
            }

            Object raw = audienceData.get(key);
            if (raw instanceof Boolean bool) {
                return bool;
            }

            String rawText = normalizeText(raw == null ? null : raw.toString());
            if (rawText.equals("true")) {
                return true;
            }
            if (rawText.equals("false")) {
                return false;
            }
        }

        return false;
    }

    private BigDecimal parseAudienceBalance(Map<String, Object> audienceData, String... keys) {
        for (String key : keys) {
            if (!audienceData.containsKey(key)) {
                continue;
            }

            Object raw = audienceData.get(key);
            if (raw == null) {
                continue;
            }

            try {
                return new BigDecimal(raw.toString().trim());
            } catch (NumberFormatException ex) {
                throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
            }
        }

        return ZERO;
    }

    private List<String> parseAssetSymbols(Map<String, Object> audienceData) {
        return parseTokenList(audienceData, FILTER_KEY_ASSET, FILTER_KEY_ASSET_SYMBOL, FILTER_KEY_ASSETS).stream()
                .map(this::normalizeAsset)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeAsset(String asset) {
        return asset == null ? "" : asset.trim().toUpperCase(Locale.ROOT);
    }

    private Set<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        return Set.of(value.trim().toUpperCase(Locale.ROOT).split("\\s+"));
    }

    private Set<String> parseTokenSet(Map<String, Object> audienceData, String... keys) {
        return parseTokenList(audienceData, keys).stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> parseTokenList(Map<String, Object> audienceData, String... keys) {
        if (audienceData == null || audienceData.isEmpty()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (String key : keys) {
            if (!audienceData.containsKey(key)) {
                continue;
            }

            values.addAll(parseTokenList(audienceData.get(key)));
        }
        return values;
    }

    private List<String> parseTokenList(Object raw) {
        if (raw == null) {
            return List.of();
        }

        if (raw instanceof List<?> values) {
            return values.stream()
                    .map(value -> value == null ? "" : value.toString())
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        return List.of(raw.toString().trim().isBlank() ? "" : raw.toString())
                .stream()
                .flatMap(value -> List.of(value.split(",")).stream())
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private List<Long> parseAudienceUserIds(Map<String, Object> audienceData, String... keys) {
        return parseTokenList(audienceData, keys).stream()
                .map(this::parseUserId)
                .filter(uid -> uid > 0)
                .distinct()
                .toList();
    }

    private List<Long> parseAudienceUserIds(Map<String, Object> audienceData) {
        return parseAudienceUserIds(audienceData, FILTER_KEY_USER_IDS);
    }

    private String normalizeText(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase(Locale.ROOT);
    }

    private Long parseUserId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(BusinessErrorCode.INVALID_AUDIENCE);
        }
    }

    private MessageTemplateSnapshot resolveDefaultTemplateFromAnnouncement(String templateCode, Map<String, Object> templateVars, String summary) {
        return resolveDefaultTemplate(templateCode, templateVars, summary);
    }

    private MessageCenterUserDtos.MessageSendOutcome publishAnnouncementNow(
            MessageCenterAnnouncement announcement,
            MessageTemplateSnapshot template,
            Map<String, Object> templateVars,
            String dedupeKey
    ) {
        MessageCreationResult messageResult = upsertMessage(
                dedupeKey,
                announcement.getTitle(),
                announcement.getSummary(),
                template.body(),
                MessageCategory.parse(announcement.getCategory()),
                MessageSeverity.parse(announcement.getSeverity()),
                announcement.getTemplateCode(),
                templateVars,
                Map.of(),
                null,
                announcement.getCreatedByType(),
                announcement.getCreatedBy(),
                null,
                null,
                null,
                announcement.getActionUrl(),
                announcement.getActionLabel(),
                announcement.getSendAt() == null ? Instant.now(clock) : announcement.getSendAt(),
                Instant.now(clock),
                announcement.getExpireAt()
        );

        List<Long> recipients = resolveRecipients(
                announcement.getAudienceType(),
                parseAudienceUserIds(parseJsonMap(announcement.getAudienceData())),
                parseJsonMap(announcement.getAudienceData())
        );

        MessageDispatchStats stats = sendToRecipients(messageResult.message(), recipients, Instant.now(clock));
        return new MessageCenterUserDtos.MessageSendOutcome(
                messageResult.message().getId(),
                messageResult.message().getTitle(),
                messageResult.message().getCategory(),
                messageResult.message().getSeverity(),
                stats.sentTo(),
                stats.skippedTo(),
                messageResult.existed(),
                messageResult.message().getEffectiveAt()
        );
    }

    private Map<String, Object> createAudienceData(List<Long> recipients, MessageAudienceType audienceType,
                                                  Map<String, Object> rawAudienceData) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("audienceType", audienceType.name());
        if (audienceType == MessageAudienceType.USER_IDS) {
            result.put("userIds", recipients == null ? List.of() : recipients);
        }

        if (rawAudienceData != null) {
            rawAudienceData.forEach((key, value) -> {
                if ("userIds".equals(key) && audienceType == MessageAudienceType.USER_IDS) {
                    return;
                }
                result.put(key, value);
            });
        }

        return result;
    }

    private MessageCenterWebSocketEvent messageNewEvent(MessageCenterMessage message) {
        return new MessageCenterWebSocketEvent(
                message.getId(),
                message.getTitle(),
                message.getSummary(),
                message.getCategory(),
                message.getSeverity(),
                message.getCreatedAt(),
                isExpired(message, Instant.now(clock)),
                false,
                message.isScheduled(),
                message.getActionUrl(),
                message.getActionLabel()
        );
    }

    private String resolveEventDedupeKey(
            String eventType,
            String eventId,
            long sourceUserId,
            String providedDedupeKey
    ) {
        if (providedDedupeKey != null && !providedDedupeKey.isBlank()) {
            return providedDedupeKey.trim();
        }
        return eventType + ":" + eventId + ":" + sourceUserId;
    }

    private record MessageCreationResult(
            MessageCenterMessage message,
            boolean existed
    ) {
    }

    private record MessageTemplateSnapshot(String title, String summary, String body) {
    }
}
