/*
 * 檔案用途：訊息中心管理端 API（公告建立、排程取消、系統事件發訊）。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MessageCenterService;
import com.example.exchange.interfaces.web.dto.MessageCenterUserDtos;
import com.example.exchange.interfaces.web.exception.BusinessException;
import com.example.exchange.interfaces.web.interceptor.ApiAuthenticationInterceptor;
import com.example.exchange.interfaces.web.security.ApiPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageCenterAdminController {

    private static final String ADMIN_ACTOR_TYPE = "ADMIN";
    private static final String SYSTEM_ACTOR_TYPE = "SYSTEM";

    private final MessageCenterService messageCenterService;

    @PostMapping("/admin/messages/announcements")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageAnnouncementCreateResponse> createAnnouncement(
            @RequestBody MessageCenterUserDtos.MessageAnnouncementCreateRequest request,
            HttpServletRequest requestContext
    ) {
        ApiPrincipal principal = resolvePrincipal(requestContext);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.createAnnouncement(
                        request,
                        principal.subject()
                )
        );
    }

    @PostMapping("/admin/messages/announcements/{announcementId}/cancel")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageAnnouncementCancelResponse> cancelAnnouncement(
            @PathVariable String announcementId
    ) {
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.cancelScheduledAnnouncement(announcementId)
        );
    }

    @PostMapping("/system/messages/send")
    public com.example.exchange.interfaces.web.dto.ApiResponse<MessageCenterUserDtos.MessageSendOutcome> sendSystemMessage(
            @RequestBody MessageCenterUserDtos.MessageSystemSendRequest request,
            HttpServletRequest requestContext
    ) {
        ApiPrincipal principal = resolvePrincipal(requestContext);
        return com.example.exchange.interfaces.web.dto.ApiResponse.ok(
                messageCenterService.sendSystemMessage(
                        request,
                        principal.subject(),
                        principalHasAdminScope(principal) ? SYSTEM_ACTOR_TYPE : ADMIN_ACTOR_TYPE
                )
        );
    }

    private ApiPrincipal resolvePrincipal(HttpServletRequest requestContext) {
        Object raw = requestContext.getAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE);
        if (raw instanceof ApiPrincipal principal) {
            return principal;
        }
        throw new BusinessException(com.example.exchange.interfaces.web.exception.BusinessErrorCode.AUTH_INVALID_CREDENTIAL);
    }

    private boolean principalHasAdminScope(ApiPrincipal principal) {
        return principal.scopes().stream()
                .map(String::toLowerCase)
                .anyMatch(scope -> Objects.equals("admin", scope) || scope.startsWith("admin:"));
    }
}
