package com.lumix.user.auth.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.api.error.ApiException;
import com.lumix.admin.superadmin.SuperAdminActivationPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.ResettableCredential;
import com.lumix.user.auth.persistence.UserAuthenticationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** 後台專用忘記密碼的安全測試，避免一般使用者帳號或 token 混入管理員復原流程。 */
class UserAuthenticationServiceSuperAdminPasswordRecoveryTest {

    @Test
    void nonAdminEmailHasIndistinguishableResponseButDoesNotReceiveRecoveryToken() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        PasswordResetDeliveryPort delivery = mock(PasswordResetDeliveryPort.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@lumix.example", "一般使用者");
        when(delivery.isAvailable()).thenReturn(true);
        when(repository.findActiveUserByEmail(user.email())).thenReturn(Optional.of(user));
        UserAuthenticationService service = service(repository, delivery, mock(SuperAdminActivationPort.class));

        service.requestSuperAdminPasswordReset(user.email());

        // endpoint 會回 accepted，但資料層與 email adapter 都不可留下任何可被一般帳戶取得的後台 token。
        verify(repository, never()).createPasswordReset(any(), anyString(), anyString(), any());
        verify(delivery, never()).deliverSuperAdminPasswordRecovery(any(), any());
    }

    @Test
    void activeSuperAdminReceivesDedicatedRecoveryDelivery() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        PasswordResetDeliveryPort delivery = mock(PasswordResetDeliveryPort.class);
        SuperAdminActivationPort superAdmin = mock(SuperAdminActivationPort.class);
        AuthenticatedUser user = new AuthenticatedUser("admin-1", "admin@lumix.example", "最高管理員");
        when(delivery.isAvailable()).thenReturn(true);
        when(repository.findActiveUserByEmail(user.email())).thenReturn(Optional.of(user));
        when(superAdmin.isActiveSuperAdmin(user.userId())).thenReturn(true);
        UserAuthenticationService service = service(repository, delivery, superAdmin);

        service.requestSuperAdminPasswordReset(user.email());

        // token 的原文只交給指定 delivery port；repository 只收到 digest，且專用信件不可退回一般重設流程。
        verify(repository).createPasswordReset(any(), anyString(), anyString(), any());
        verify(delivery).deliverSuperAdminPasswordRecovery(any(AuthenticatedUser.class), any());
        verify(delivery, never()).deliver(any(), any());
    }

    @Test
    void regularUserResetTokenCannotBeConsumedAtAdminResetEndpoint() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        PasswordResetDeliveryPort delivery = mock(PasswordResetDeliveryPort.class);
        SuperAdminActivationPort superAdmin = mock(SuperAdminActivationPort.class);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@lumix.example", "一般使用者");
        when(repository.lockActivePasswordReset(anyString())).thenReturn(Optional.of(new ResettableCredential(UUID.randomUUID(), user)));
        when(superAdmin.isPasswordResetEligibleSuperAdmin(user.userId())).thenReturn(false);
        UserAuthenticationService service = service(repository, delivery, superAdmin);

        assertThrows(ApiException.class, () -> service.resetSuperAdminPassword("valid-reset-token", "new-password-123"));

        // 拒絕時不可修改密碼、撤銷 session 或消耗一般帳戶的 token；transaction 也會保護鎖定的列不被誤用。
        verify(repository, never()).updatePasswordHash(anyString(), anyString());
        verify(repository, never()).revokeAllSessions(anyString());
        verify(repository, never()).consumePasswordReset(any());
    }

    @Test
    void pendingSuperAdminActivationTokenCanSetPasswordAndActivateTheExistingPrincipal() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        PasswordResetDeliveryPort delivery = mock(PasswordResetDeliveryPort.class);
        SuperAdminActivationPort superAdmin = mock(SuperAdminActivationPort.class);
        AuthenticatedUser user = new AuthenticatedUser("admin-1", "admin@lumix.example", "最高管理員");
        when(repository.lockActivePasswordReset(anyString())).thenReturn(Optional.of(new ResettableCredential(UUID.randomUUID(), user)));
        when(superAdmin.isPasswordResetEligibleSuperAdmin(user.userId())).thenReturn(true);
        UserAuthenticationService service = service(repository, delivery, superAdmin);

        assertDoesNotThrow(() -> service.resetSuperAdminPassword("activation-reset-token", "new-password-123"));

        // 啟用連結與既有復原連結共用後台頁面，但只能在消耗有效 token 後原子地更新密碼、撤銷 session 並啟用 principal。
        verify(repository).updatePasswordHash(anyString(), any());
        verify(repository).revokeAllSessions(user.userId());
        verify(repository).consumePasswordReset(any());
        verify(superAdmin).activateIfPending(user.userId());
    }

    private static UserAuthenticationService service(
        UserAuthenticationRepository repository,
        PasswordResetDeliveryPort passwordResetDelivery,
        SuperAdminActivationPort superAdminActivation
    ) {
        return new UserAuthenticationService(
            repository,
            mock(RegistrationEmailBloomFilter.class),
            mock(BCryptPasswordEncoder.class),
            passwordResetDelivery,
            mock(RegistrationVerificationDeliveryPort.class),
            mock(LoginVerificationDeliveryPort.class),
            superAdminActivation,
            new UserAuthenticationProperties(),
            Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
