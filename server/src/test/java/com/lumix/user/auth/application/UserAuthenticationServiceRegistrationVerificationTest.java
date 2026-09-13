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
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.PendingRegistration;
import com.lumix.user.auth.domain.RegistrationVerificationSecret;
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

    private static UserAuthenticationService service(
        UserAuthenticationRepository repository,
        RegistrationEmailBloomFilter bloomFilter,
        BCryptPasswordEncoder passwordEncoder,
        RegistrationVerificationDeliveryPort delivery
    ) {
        return new UserAuthenticationService(
            repository, bloomFilter, passwordEncoder, mock(PasswordResetDeliveryPort.class), delivery,
            mock(LoginVerificationDeliveryPort.class), new UserAuthenticationProperties(),
            Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
