package com.lumix.user.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.api.error.ApiException;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.admin.superadmin.SuperAdminActivationPort;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.PendingRegistration;
import com.lumix.user.auth.domain.RegistrationVerificationSecret;
import com.lumix.user.auth.domain.SessionSecret;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.persistence.UserAuthenticationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserAuthenticationServiceRegistrationVerificationTest {

    @Test
    void onlyBothEmailCodesCreateTheAccountAndSession() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        RegistrationEmailBloomFilter bloomFilter = mock(RegistrationEmailBloomFilter.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        RegistrationVerificationDeliveryPort delivery = mock(RegistrationVerificationDeliveryPort.class);
        when(delivery.isAvailable()).thenReturn(true);
        when(passwordEncoder.encode("correct-password")).thenReturn("bcrypt-hash");
        UserAuthenticationService service = service(repository, bloomFilter, passwordEncoder, delivery);

        UserAuthenticationService.RegistrationVerificationRequested requested = service.requestRegistrationVerification(
            "new@example.com", "New User", "correct-password"
        );
        ArgumentCaptor<PendingRegistration> pendingCaptor = ArgumentCaptor.forClass(PendingRegistration.class);
        ArgumentCaptor<RegistrationVerificationSecret> secretCaptor = ArgumentCaptor.forClass(RegistrationVerificationSecret.class);
        verify(repository).upsertRegistrationVerification(pendingCaptor.capture());
        verify(delivery).deliver(anyString(), secretCaptor.capture());
        PendingRegistration pending = pendingCaptor.getValue();
        RegistrationVerificationSecret secret = secretCaptor.getValue();
        when(repository.lockActiveRegistrationVerification(requested.registrationId())).thenReturn(Optional.of(pending));

        LoginRequestMetadata metadata = new LoginRequestMetadata("203.0.113.3", "測試瀏覽器", "browser-digest");
        UserAuthenticationService.AuthenticationResult result = service.completeRegistrationVerification(
            requested.registrationId(), secret.numericCode(), secret.letterCode().toLowerCase(), metadata
        );

        // 註冊成功的最低不變式：雙碼正確後才寫入帳號、受信任裝置、session 並消耗短時效申請。
        assertEquals(pending.user(), result.user());
        verify(repository).createUser(pending.user(), "bcrypt-hash");
        verify(repository).createTrustedDevice(
            result.device().deviceId(), pending.user().userId(), result.device().secretDigest(), metadata
        );
        verify(repository).consumeRegistrationVerification(requested.registrationId());
    }

    @Test
    void wrongEitherCodeConsumesAnAttemptAndNeverCreatesUser() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        RegistrationEmailBloomFilter bloomFilter = mock(RegistrationEmailBloomFilter.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        RegistrationVerificationDeliveryPort delivery = mock(RegistrationVerificationDeliveryPort.class);
        when(delivery.isAvailable()).thenReturn(true);
        when(passwordEncoder.encode("correct-password")).thenReturn("bcrypt-hash");
        UserAuthenticationService service = service(repository, bloomFilter, passwordEncoder, delivery);
        UserAuthenticationService.RegistrationVerificationRequested requested = service.requestRegistrationVerification(
            "new@example.com", "New User", "correct-password"
        );
        ArgumentCaptor<PendingRegistration> pendingCaptor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(repository).upsertRegistrationVerification(pendingCaptor.capture());
        when(repository.lockActiveRegistrationVerification(requested.registrationId())).thenReturn(Optional.of(pendingCaptor.getValue()));

        // 即使其中一組正確，另一組錯誤仍不可建立帳號，且必須計入暴力嘗試限制。
        assertThrows(ApiException.class, () -> service.completeRegistrationVerification(
            requested.registrationId(), "000000", "WRONG", new LoginRequestMetadata("203.0.113.3", "測試瀏覽器", "browser-digest")
        ));
        verify(repository).recordRegistrationVerificationFailure(requested.registrationId(), 5);
        verify(repository, org.mockito.Mockito.never()).createUser(any(), anyString());
    }

    @Test
    void authenticatedPasswordChangeIdentifiesOnlyTheWrongCurrentPassword() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        SessionSecret session = new SessionSecret(java.util.UUID.randomUUID(), "session-secret", "session-digest");
        when(repository.findActiveSession(session.sessionId(), session.secretDigest())).thenReturn(Optional.of(user));
        when(repository.findPasswordCredentialByEmail(user.email())).thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("incorrect-password", "bcrypt-hash")).thenReturn(false);
        UserAuthenticationService service = service(repository, mock(RegistrationEmailBloomFilter.class), passwordEncoder,
            mock(RegistrationVerificationDeliveryPort.class));

        ApiException exception = assertThrows(ApiException.class, () -> service.changePassword(
            session, "incorrect-password", "new-password", null,
            new LoginRequestMetadata("203.0.113.4", "測試瀏覽器", "browser-digest")
        ));

        // 只有已驗證本人且 BCrypt 不符時才能給精確提示；登入入口不應重用此錯誤碼。
        assertEquals(ApiErrorCode.CURRENT_PASSWORD_INCORRECT, exception.getErrorCode());
    }

    @Test
    void authenticatedPasswordChangeRejectsARepeatedCurrentPassword() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@example.com", "User");
        SessionSecret session = new SessionSecret(java.util.UUID.randomUUID(), "session-secret", "session-digest");
        when(repository.findActiveSession(session.sessionId(), session.secretDigest())).thenReturn(Optional.of(user));
        when(repository.findPasswordCredentialByEmail(user.email())).thenReturn(Optional.of(new PasswordCredential(user, "bcrypt-hash")));
        when(passwordEncoder.matches("current-password", "bcrypt-hash")).thenReturn(true);
        UserAuthenticationService service = service(repository, mock(RegistrationEmailBloomFilter.class), passwordEncoder,
            mock(RegistrationVerificationDeliveryPort.class));

        ApiException exception = assertThrows(ApiException.class, () -> service.changePassword(
            session, "current-password", "current-password", null,
            new LoginRequestMetadata("203.0.113.4", "測試瀏覽器", "browser-digest")
        ));

        // 已驗證舊密碼後才允許說明新舊相同，且不得寫入相同 hash 或撤銷既有 session。
        assertEquals(ApiErrorCode.NEW_PASSWORD_SAME_AS_CURRENT, exception.getErrorCode());
        verify(repository, org.mockito.Mockito.never()).updatePasswordHash(anyString(), anyString());
    }

    private static UserAuthenticationService service(
        UserAuthenticationRepository repository,
        RegistrationEmailBloomFilter bloomFilter,
        BCryptPasswordEncoder passwordEncoder,
        RegistrationVerificationDeliveryPort delivery
    ) {
        return new UserAuthenticationService(
            repository, bloomFilter, passwordEncoder, mock(PasswordResetDeliveryPort.class), delivery,
            mock(LoginVerificationDeliveryPort.class), mock(SuperAdminActivationPort.class), new UserAuthenticationProperties(),
            Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
