/*
 * File purpose: Tests for local exchange registration, login, and logout.
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.AppUserRecord;
import com.example.exchange.domain.model.entity.AuthRefreshSessionRecord;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.jpa.AppUserRecordJpaRepository;
import com.example.exchange.domain.repository.jpa.AuthRefreshSessionRecordJpaRepository;
import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.interfaces.web.security.JwtAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    @DisplayName("register creates a local user, creates exchange account, and returns JWT-compatible token")
    void registerCreatesUserAccountAndToken() {
        Fixture fixture = new Fixture();
        // Scenario: a brand-new first-party user registers and should immediately be able to authenticate.
        when(fixture.users.existsByEmail("alice@example.com")).thenReturn(false);
        when(fixture.users.saveAndFlush(any(AppUserRecord.class))).thenAnswer(invocation -> {
            AppUserRecord user = invocation.getArgument(0);
            user.setId(10001L);
            return user;
        });
        when(fixture.sessions.save(any(AuthRefreshSessionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.AuthResult result = fixture.service.register("Alice@Example.com", "correct-password");

        // Expected result: email is normalized, the uid-backed exchange account is created, and JWT is valid.
        assertThat(result.user().uid()).isEqualTo(10001L);
        assertThat(result.user().email()).isEqualTo("alice@example.com");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(new JwtAuthenticator(fixture.objectMapper, fixture.properties.getJwtHmacSecret(), 60)
                .authenticate(result.accessToken()))
                .isPresent()
                .get()
                .extracting(principal -> principal.subject())
                .isEqualTo("10001");
        verify(fixture.accountRepository).save(any());
    }

    @Test
    @DisplayName("register rejects duplicate email")
    void registerRejectsDuplicateEmail() {
        Fixture fixture = new Fixture();
        // Scenario: registration must not allow duplicate login identifiers.
        when(fixture.users.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> fixture.service.register("alice@example.com", "correct-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already registered");
    }

    @Test
    @DisplayName("login rejects inactive or invalid credentials")
    void loginRejectsInvalidCredentials() {
        Fixture fixture = new Fixture();
        // Scenario: invalid passwords must fail with a generic error to avoid account enumeration.
        AppUserRecord user = new AppUserRecord("alice@example.com", fixture.passwordHashService.hash("correct-password"));
        user.setId(10001L);
        when(fixture.users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> fixture.service.login("alice@example.com", "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid credentials");
    }

    @Test
    @DisplayName("logout revokes the refresh session when token hash matches")
    void logoutRevokesRefreshSession() {
        Fixture fixture = new Fixture();
        AtomicReference<AuthRefreshSessionRecord> savedSession = new AtomicReference<>();
        // Scenario: logout receives the raw refresh token and revokes only its stored hash.
        when(fixture.users.existsByEmail("alice@example.com")).thenReturn(false);
        when(fixture.users.saveAndFlush(any(AppUserRecord.class))).thenAnswer(invocation -> {
            AppUserRecord user = invocation.getArgument(0);
            user.setId(10001L);
            return user;
        });
        when(fixture.sessions.save(any(AuthRefreshSessionRecord.class))).thenAnswer(invocation -> {
            AuthRefreshSessionRecord session = invocation.getArgument(0);
            savedSession.set(session);
            return session;
        });

        AuthService.AuthResult result = fixture.service.register("alice@example.com", "correct-password");
        when(fixture.sessions.findByRefreshTokenHash(savedSession.get().getRefreshTokenHash()))
                .thenReturn(Optional.of(savedSession.get()));

        assertThat(fixture.service.logout(result.refreshToken())).isTrue();
        assertThat(savedSession.get().getRevokedAt()).isBeforeOrEqualTo(Instant.now());
    }

    private static final class Fixture {
        private final AppUserRecordJpaRepository users = mock(AppUserRecordJpaRepository.class);
        private final AuthRefreshSessionRecordJpaRepository sessions = mock(AuthRefreshSessionRecordJpaRepository.class);
        private final AccountRepository accountRepository = mock(AccountRepository.class);
        private final PasswordHashService passwordHashService = new PasswordHashService();
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final ApiAuthProperties properties = new ApiAuthProperties();
        private final JwtTokenService jwtTokenService;
        private final AuthService service;

        private Fixture() {
            properties.setJwtHmacSecret("test-secret-for-auth-service");
            jwtTokenService = new JwtTokenService(properties, objectMapper);
            service = new AuthService(
                    users,
                    sessions,
                    accountRepository,
                    passwordHashService,
                    jwtTokenService,
                    objectMapper
            );
        }
    }
}
