/*
 * 檔案用途：訊息中心使用者 API（列表、詳情、狀態操作、偏好設定）。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MessageCenterService;
import com.example.exchange.interfaces.web.dto.MessageCenterUserDtos;
import com.example.exchange.interfaces.web.exception.BusinessErrorCode;
import com.example.exchange.interfaces.web.exception.BusinessException;
import com.example.exchange.interfaces.web.interceptor.ApiAuthenticationInterceptor;
import com.example.exchange.interfaces.web.security.ApiPrincipal;
import com.example.exchange.interfaces.web.security.UserStreamSubscriptionAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageCenterController {

    private final MessageCenterService messageCenterService;

    @GetMapping("/messages")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageListResponse> listMessages(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "status", defaultValue = "ALL") String status,
            @RequestParam(value = "archived", defaultValue = "false") boolean archived,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "pinnedFirst", defaultValue = "true") boolean pinnedFirst,
            @RequestParam(value = "excludeDeleted", defaultValue = "true") boolean excludeDeleted,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.listMessages(
                        uid,
                        category,
                        search,
                        status,
                        archived,
                        cursor,
                        limit,
                        pinnedFirst,
                        excludeDeleted
                )
        );
    }

    @GetMapping("/messages/{messageId}")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageDetailResponse> getMessageDetail(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.getMessageDetail(uid, messageId, false)
        );
    }

    @PostMapping("/messages/{messageId}/read")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageActionResponse> readMessage(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.markRead(uid, messageId));
    }

    @PostMapping("/messages/read-all")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageReadAllResponse> readAllMessages(
            @RequestBody(required = false) MessageCenterUserDtos.MessageReadAllRequest request,
            HttpServletRequest requestContext
    ) {
        long uid = resolveUid(requestContext);
        String scope = request == null || request.scope() == null ? "ALL" : request.scope();
        String category = request == null ? null : request.category();
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.markAllRead(uid, scope, category)
        );
    }

    @PostMapping("/messages/{messageId}/archive")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageActionResponse> archiveMessage(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.archive(uid, messageId));
    }

    @PostMapping("/messages/{messageId}/unarchive")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageActionResponse> unarchiveMessage(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.unarchive(uid, messageId));
    }

    @PostMapping("/messages/{messageId}/pin")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageActionResponse> pinMessage(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.pin(uid, messageId, true));
    }

    @DeleteMapping("/messages/{messageId}/pin")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageActionResponse> unpinMessage(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.pin(uid, messageId, false));
    }

    @DeleteMapping("/messages/{messageId}")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageActionResponse> deleteMessage(
            @PathVariable String messageId,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.softDelete(uid, messageId));
    }

    @GetMapping("/messages/unread-count")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageUnreadCountResponse> unreadCount(
            @RequestParam(value = "excludeArchived", defaultValue = "true") boolean excludeArchived,
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.getUnreadCount(uid, excludeArchived));
    }

    @GetMapping("/message-preferences")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessagePreferencesResponse> getPreferences(
            HttpServletRequest request
    ) {
        long uid = resolveUid(request);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(messageCenterService.getPreferences(uid));
    }

    @PutMapping("/message-preferences")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessagePreferenceUpdateResponse> updatePreferences(
            @RequestBody(required = false) MessageCenterUserDtos.MessagePreferenceUpdateRequest request,
            HttpServletRequest requestContext
    ) {
        ApiPrincipal principal = resolvePrincipal(requestContext);
        long uid = resolveUidFromPrincipal(principal);
        String actor = principal.subject();

        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.updatePreferences(uid, actor, request)
        );
    }

    private long resolveUid(HttpServletRequest request) {
        ApiPrincipal principal = resolvePrincipal(request);
        return resolveUidFromPrincipal(principal);
    }

    private long resolveUidFromPrincipal(ApiPrincipal principal) {
        return UserStreamSubscriptionAuthorizer.parseSubjectUid(principal.subject())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.AUTH_INVALID_CREDENTIAL));
    }

    private ApiPrincipal resolvePrincipal(HttpServletRequest request) {
        Object raw = request.getAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE);
        if (raw instanceof ApiPrincipal principal) {
            return principal;
        }
        throw new BusinessException(BusinessErrorCode.AUTH_INVALID_CREDENTIAL);
    }
}
