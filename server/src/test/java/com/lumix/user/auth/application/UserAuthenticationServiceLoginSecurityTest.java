package com.lumix.user.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.admin.superadmin.SuperAdminActivationPort;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationRequest;
import com.lumix.user.auth.domain.LoginVerificationState;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.persistence.UserAuthenticationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserAuthenticationServiceLoginSecurityTest {

    @Test
    void activeSuperAdminUsesDedicatedSessionWithoutDeviceVerification() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        SuperAdminActivationPort superAdmin = mock(SuperAdminActivationPort.class);
        AuthenticatedUser user = new AuthenticatedUser("admin-1", "admin@lumix.example", "管理員");
        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.25", "管理瀏覽器", "admin-browser-digest");
        when(repository.findPasswordCredentialByEmail(user.email()))
            .thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(superAdmin.isActiveSuperAdmin(user.userId())).thenReturn(true);
        UserAuthenticationService service = new UserAuthenticationService(
            repository, mock(RegistrationEmailBloomFilter.class), passwordEncoder, mock(PasswordResetDeliveryPort.class),
            mock(RegistrationVerificationDeliveryPort.class), mock(LoginVerificationDeliveryPort.class), superAdmin,
            new UserAuthenticationProperties(), Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)
        );

        UserAuthenticationService.AuthenticationResult result = service.loginActiveSuperAdmin(
            user.email(), "correct-password", metadata
        );

        // 管理登入不應建立或信任前台裝置；只有通過 ACTIVE principal 查驗才可寫入專用 session。
        assertNotNull(result.session());
        assertEquals(null, result.device());
        verify(repository).createSession(any(), eq(user.userId()), anyString(), any(), isNull(), eq(metadata));
        verify(repository, never()).createTrustedDevice(any(), any(), any(), any());
    }

    @Test
    void emptyPlatformSlotBindsUnknownDeviceWithoutEmail() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        LoginVerificationDeliveryPort delivery = mock(LoginVerificationDeliveryPort.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.8", "新瀏覽器", "browser-digest");
        when(repository.findPasswordCredentialByEmail(user.email()))
            .thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(repository.lockActiveUserForBoundDeviceChange(user.userId())).thenReturn(true);
        when(repository.hasActiveBoundLoginDeviceForPlatform(user.userId(), metadata.devicePlatform())).thenReturn(false);
        UserAuthenticationService service = service(repository, passwordEncoder, delivery);

        UserAuthenticationService.LoginResult result = service.login(user.email(), "correct-password", null, metadata);

        // 沒有同類別裝置時可直接填入空槽位，但仍必須寫入可信裝置與成功 session evidence。
        assertEquals(false, result.requiresVerification());
        assertNotNull(result.authentication().device());
        verify(repository).createTrustedDevice(
            result.authentication().device().deviceId(), user.userId(), result.authentication().device().secretDigest(), metadata
        );
        verify(delivery, never()).deliver(any(), any(), any());
    }

    @Test
    void existingPlatformSlotRequiresEmailApproval() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        LoginVerificationDeliveryPort delivery = mock(LoginVerificationDeliveryPort.class);
        AuthenticatedUser user = new AuthenticatedUser("first-user", "first@example.com", "First User");
        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.11", "第一個裝置", "first-device-digest");
        when(repository.findPasswordCredentialByEmail(user.email()))
            .thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(repository.lockActiveUserForBoundDeviceChange(user.userId())).thenReturn(true);
        when(repository.hasActiveBoundLoginDeviceForPlatform(user.userId(), metadata.devicePlatform())).thenReturn(true);
        when(delivery.isAvailable()).thenReturn(true);
        UserAuthenticationProperties properties = new UserAuthenticationProperties();
        properties.getLoginVerification().setEnabled(true);

        UserAuthenticationService.LoginResult result = service(repository, passwordEncoder, delivery, properties)
            .login(user.email(), "correct-password", null, metadata);

        // 同一類別已占用時，任何不同 device cookie 都必須先取得 email 核准，不能由舊通知開關繞過。
        assertEquals(true, result.requiresVerification());
        verify(delivery).deliver(eq(user), eq(metadata), any());
        verify(repository, never()).createTrustedDevice(any(), any(), any(), any());
    }

    @Test
    void disabledLoginVerificationReplacesDeviceWithoutEmailApproval() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        AuthenticatedUser user = new AuthenticatedUser("first-user", "first@example.com", "First User");
        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.11", "第一個裝置", "first-device-digest");
        when(repository.findPasswordCredentialByEmail(user.email()))
            .thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(repository.lockActiveUserForBoundDeviceChange(user.userId())).thenReturn(true);
        when(repository.hasActiveBoundLoginDeviceForPlatform(user.userId(), metadata.devicePlatform())).thenReturn(true);
        when(repository.extendFundTransferRestriction(eq(user.userId()), any())).thenReturn(true);
        UserAuthenticationProperties properties = new UserAuthenticationProperties();
        properties.getLoginVerification().setEnabled(false);

        UserAuthenticationService.LoginResult result = service(repository, passwordEncoder, mock(LoginVerificationDeliveryPort.class), properties)
            .login(user.email(), "correct-password", null, metadata);

        // 停用確認只省略 email 核准，換機的撤銷、資金限制、可信裝置與 session 證據仍須完整保留。
        assertFalse(result.requiresVerification());
        assertNotNull(result.authentication().device());
        verify(repository).revokeActiveBoundDevicesForPlatform(user.userId(), metadata.devicePlatform());
        verify(repository).extendFundTransferRestriction(eq(user.userId()), any());
        verify(repository).createTrustedDevice(any(), eq(user.userId()), anyString(), eq(metadata));
    }

    @Test
    void removingBoundDeviceRevokesSessionsUsingThatDevice() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        UUID deviceId = UUID.randomUUID();
        when(repository.revokeBoundLoginDevice(user.userId(), deviceId)).thenReturn(true);

        service(repository).removeBoundLoginDevice(user, deviceId);

        // 清單上的移除必須同時中止該 device 已建立的 session，否則只是視覺上的假移除。
        verify(repository).revokeSessionsForDevice(user.userId(), deviceId);
    }

    @Test
    void emailYesOnlyApprovesOriginalCandidateAndDoesNotBindConfirmationBrowser() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        LoginVerificationRequest request = new LoginVerificationRequest(
            UUID.randomUUID(), user, UUID.randomUUID(), "candidate-device-digest",
            new LoginRequestMetadata("198.51.100.10", "原始登入裝置", "original-browser-digest"),
            LoginVerificationState.PENDING, Instant.parse("2026-09-11T00:15:00Z")
        );
        when(repository.lockActiveLoginVerificationByApprovalToken(anyString())).thenReturn(Optional.of(request));
        when(repository.decideLoginVerification(request.verificationRequestId(), true)).thenReturn(true);
        UserAuthenticationService service = service(repository);

        LoginVerificationState state = service.decideLoginVerification("one-time-email-token", true);

        // email link 只改變核准狀態；不應把閱讀 email 的任意瀏覽器變成同類別受信任裝置。
        assertEquals(LoginVerificationState.APPROVED, state);
        verify(repository, never()).createTrustedDevice(any(), any(), any(), any());
        verify(repository, never()).createSession(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approvedCandidateReplacesExistingDeviceInSamePlatformSlot() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        LoginRequestMetadata candidate = new LoginRequestMetadata("198.51.100.10", "原始登入裝置", "original-browser-digest");
        UUID candidateDeviceId = UUID.randomUUID();
        LoginVerificationRequest request = new LoginVerificationRequest(
            UUID.randomUUID(), user, candidateDeviceId, "candidate-device-digest", candidate,
            LoginVerificationState.APPROVED, Instant.parse("2026-09-11T00:15:00Z")
        );
        when(repository.lockActiveLoginVerificationByPendingToken(request.verificationRequestId(), "pending-digest"))
            .thenReturn(Optional.of(request));
        when(repository.consumeApprovedLoginVerification(request.verificationRequestId())).thenReturn(true);
        when(repository.lockActiveUserForBoundDeviceChange(user.userId())).thenReturn(true);
        when(repository.extendFundTransferRestriction(user.userId(), Instant.parse("2026-09-12T00:00:00Z"))).thenReturn(true);

        UserAuthenticationService.LoginVerificationCompletion completion = service(repository).completeLoginVerification(
            new com.lumix.user.auth.domain.PendingLoginVerificationSecret(
                request.verificationRequestId(), "pending-secret", "pending-digest",
                new com.lumix.user.auth.domain.DeviceSecret(
                    candidateDeviceId, "candidate-device-secret", "candidate-device-digest"
                )
            ),
            candidate
        );

        // 原始候選裝置取得該類別唯一槽位前，必須撤銷舊裝置與其 session；不依賴 email 閱讀端的 metadata。
        assertEquals(LoginVerificationState.APPROVED, completion.state());
        verify(repository).revokeActiveBoundDevicesForPlatform(user.userId(), candidate.devicePlatform());
        verify(repository).extendFundTransferRestriction(user.userId(), Instant.parse("2026-09-12T00:00:00Z"));
        verify(repository).createTrustedDevice(candidateDeviceId, user.userId(), "candidate-device-digest", candidate);
    }

    private static UserAuthenticationService service(UserAuthenticationRepository repository) {
        return service(repository, mock(BCryptPasswordEncoder.class), mock(LoginVerificationDeliveryPort.class));
    }

    private static UserAuthenticationService service(
        UserAuthenticationRepository repository,
        BCryptPasswordEncoder passwordEncoder,
        LoginVerificationDeliveryPort loginVerificationDelivery
    ) {
        return service(repository, passwordEncoder, loginVerificationDelivery, new UserAuthenticationProperties());
    }

    private static UserAuthenticationService service(
        UserAuthenticationRepository repository,
        BCryptPasswordEncoder passwordEncoder,
        LoginVerificationDeliveryPort loginVerificationDelivery,
        UserAuthenticationProperties properties
    ) {
        return new UserAuthenticationService(
            repository,
            mock(RegistrationEmailBloomFilter.class),
            passwordEncoder,
            mock(PasswordResetDeliveryPort.class),
            loginVerificationDelivery,
            properties,
            Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
