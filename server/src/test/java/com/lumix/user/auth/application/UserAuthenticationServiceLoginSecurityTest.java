package com.lumix.user.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginSecuritySettings;
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
    void disabledEmailNotificationAllowsUnknownDeviceAndStillBindsIt() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        LoginVerificationDeliveryPort delivery = mock(LoginVerificationDeliveryPort.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.8", "新瀏覽器", "browser-digest");
        when(repository.findPasswordCredentialByEmail(user.email()))
            .thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(repository.findActiveLoginSecuritySettings(user.userId())).thenReturn(Optional.of(new LoginSecuritySettings(false)));
        UserAuthenticationService service = service(repository, passwordEncoder, delivery);

        UserAuthenticationService.LoginResult result = service.login(user.email(), "correct-password", null, metadata);

        // 使用者關閉通知後不應寄信卡住登入，但未知裝置仍必須寫入可信裝置與成功 session evidence。
        assertEquals(false, result.requiresVerification());
        assertNotNull(result.authentication().device());
        verify(repository).createTrustedDevice(
            result.authentication().device().deviceId(), user.userId(), result.authentication().device().secretDigest(), metadata
        );
        verify(delivery, never()).deliver(any(), any(), any());
    }

    @Test
    void firstBoundDeviceSkipsEmailEvenWhenNotificationIsEnabled() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        LoginVerificationDeliveryPort delivery = mock(LoginVerificationDeliveryPort.class);
        AuthenticatedUser user = new AuthenticatedUser("first-user", "first@example.com", "First User");
        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.11", "第一個裝置", "first-device-digest");
        when(repository.findPasswordCredentialByEmail(user.email()))
            .thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(repository.findActiveLoginSecuritySettings(user.userId())).thenReturn(Optional.of(new LoginSecuritySettings(true)));
        when(repository.hasActiveBoundLoginDevices(user.userId())).thenReturn(false);

        UserAuthenticationService.LoginResult result = service(repository, passwordEncoder, delivery)
            .login(user.email(), "correct-password", null, metadata);

        // 第一個可用裝置沒有既有安全基線，不能寄出無法判讀的「新裝置」通知。
        assertFalse(result.requiresVerification());
        verify(delivery, never()).deliver(any(), any(), any());
        verify(repository).createTrustedDevice(
            result.authentication().device().deviceId(), user.userId(), result.authentication().device().secretDigest(), metadata
        );
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
    void emailYesCreatesSessionAndNewBoundDeviceInConfirmationBrowser() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        LoginVerificationRequest request = new LoginVerificationRequest(
            UUID.randomUUID(), user, UUID.randomUUID(), "candidate-device-digest",
            new LoginRequestMetadata("198.51.100.10", "原始登入裝置", "original-browser-digest"),
            LoginVerificationState.PENDING, Instant.parse("2026-09-11T00:15:00Z")
        );
        when(repository.lockActiveLoginVerificationByApprovalToken(anyString())).thenReturn(Optional.of(request));
        when(repository.decideLoginVerification(request.verificationRequestId(), true)).thenReturn(true);
        when(repository.consumeApprovedLoginVerification(request.verificationRequestId())).thenReturn(true);
        UserAuthenticationService service = service(repository);
        LoginRequestMetadata emailBrowser = new LoginRequestMetadata("203.0.113.8", "Email 確認瀏覽器", "email-browser-digest");

        UserAuthenticationService.LoginVerificationCompletion completion = service.completeLoginVerificationByEmail(
            "one-time-email-token", true, emailBrowser
        );

        // Yes 必須在目前確認頁瀏覽器建立 session，並為每一次成功核准建立一個新的受信任裝置紀錄。
        assertEquals(LoginVerificationState.APPROVED, completion.state());
        assertNotNull(completion.authentication());
        assertNotNull(completion.authentication().device());
        verify(repository).createTrustedDevice(
            completion.authentication().device().deviceId(), user.userId(),
            completion.authentication().device().secretDigest(), emailBrowser
        );
        verify(repository).createSession(
            eq(completion.authentication().session().sessionId()), eq(user.userId()),
            eq(completion.authentication().session().secretDigest()), any(),
            eq(completion.authentication().device().deviceId()), eq(emailBrowser)
        );
    }

    private static UserAuthenticationService service(UserAuthenticationRepository repository) {
        return service(repository, mock(BCryptPasswordEncoder.class), mock(LoginVerificationDeliveryPort.class));
    }

    private static UserAuthenticationService service(
        UserAuthenticationRepository repository,
        BCryptPasswordEncoder passwordEncoder,
        LoginVerificationDeliveryPort loginVerificationDelivery
    ) {
        return new UserAuthenticationService(
            repository,
            mock(RegistrationEmailBloomFilter.class),
            passwordEncoder,
            mock(PasswordResetDeliveryPort.class),
            loginVerificationDelivery,
            new UserAuthenticationProperties(),
            Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
