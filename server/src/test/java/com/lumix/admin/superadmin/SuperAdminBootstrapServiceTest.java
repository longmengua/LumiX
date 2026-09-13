package com.lumix.admin.superadmin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.user.auth.application.PasswordResetDeliveryPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** 最高管理員 bootstrap 的安全測試，保護單一 principal 與受控 email 啟用不變式。 */
class SuperAdminBootstrapServiceTest {

    @Test
    void blankConfiguredEmailDoesNotCreateAnAdminPrincipal() {
        SuperAdminRepository repository = mock(SuperAdminRepository.class);
        SuperAdminBootstrapService service = service(repository, "", mock(PasswordResetDeliveryPort.class));

        service.bootstrap();

        // 空白表示部署沒有開啟後台 bootstrap，絕不能自動產生預設帳密或管理者。
        verify(repository, never()).lockSuperAdmin();
        verify(repository, never()).createPendingSuperAdmin(anyString());
    }

    @Test
    void newConfiguredEmailCreatesPendingPrincipalAndOnlyDeliversOneTimeLink() {
        SuperAdminRepository repository = mock(SuperAdminRepository.class);
        PasswordResetDeliveryPort delivery = mock(PasswordResetDeliveryPort.class);
        when(delivery.isAvailable()).thenReturn(true);
        when(repository.lockSuperAdmin()).thenReturn(Optional.empty());
        when(repository.findUserByEmail("root@lumix.example")).thenReturn(Optional.empty());
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-placeholder-hash");
        SuperAdminBootstrapService service = service(repository, "root@lumix.example", delivery, passwordEncoder);

        service.bootstrap();

        // 初始 credential 是拋棄明文的隨機 placeholder，啟用信 token 也只能以 digest 落庫。
        verify(repository).createBootstrapUser(any(AuthenticatedUser.class), anyString());
        verify(repository).createPendingSuperAdmin(anyString());
        verify(repository).invalidateActivePasswordResets(anyString());
        verify(repository).createPasswordReset(any(), anyString(), anyString(), any());
        verify(delivery).deliverSuperAdminActivation(any(AuthenticatedUser.class), any());
    }

    @Test
    void changingConfiguredEmailAfterPrincipalExistsFailsClosed() {
        SuperAdminRepository repository = mock(SuperAdminRepository.class);
        PasswordResetDeliveryPort delivery = mock(PasswordResetDeliveryPort.class);
        when(delivery.isAvailable()).thenReturn(true);
        when(repository.lockSuperAdmin()).thenReturn(Optional.of(new SuperAdminPrincipal(
            new AuthenticatedUser("admin-1", "original@lumix.example", "最高管理員"),
            SuperAdminPrincipal.State.ACTIVE
        )));
        SuperAdminBootstrapService service = service(repository, "attacker@lumix.example", delivery);

        // 已建立的最高管理員不可藉由改 .env 悄悄轉移給其他信箱。
        assertThrows(IllegalStateException.class, service::bootstrap);
        verify(repository, never()).createPendingSuperAdmin(anyString());
        verify(delivery, never()).deliverSuperAdminActivation(any(), any());
    }

    @Test
    void successfulPasswordResetCanOnlyActivateAnExistingPendingPrincipal() {
        SuperAdminRepository repository = mock(SuperAdminRepository.class);
        SuperAdminBootstrapService service = service(repository, "", mock(PasswordResetDeliveryPort.class));

        service.activateIfPending("admin-1");

        // 這個 port 不具建立或指派角色能力，只能原子啟用已存在的 pending principal。
        verify(repository).activatePendingSuperAdmin("admin-1");
        verify(repository, never()).createPendingSuperAdmin(anyString());
    }

    private static SuperAdminBootstrapService service(
        SuperAdminRepository repository,
        String email,
        PasswordResetDeliveryPort delivery
    ) {
        return service(repository, email, delivery, mock(BCryptPasswordEncoder.class));
    }

    private static SuperAdminBootstrapService service(
        SuperAdminRepository repository,
        String email,
        PasswordResetDeliveryPort delivery,
        BCryptPasswordEncoder passwordEncoder
    ) {
        SuperAdminProperties properties = new SuperAdminProperties();
        properties.setEmail(email);
        return new SuperAdminBootstrapService(
            repository,
            properties,
            delivery,
            passwordEncoder,
            new UserAuthenticationProperties(),
            Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
