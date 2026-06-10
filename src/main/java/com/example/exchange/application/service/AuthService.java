/*
 * File purpose: Local first-party registration, login, logout, and current-user lookup.
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.AppUserRecord;
import com.example.exchange.domain.model.entity.AuthRefreshSessionRecord;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.jpa.AppUserRecordJpaRepository;
import com.example.exchange.domain.repository.jpa.AuthRefreshSessionRecordJpaRepository;
import com.example.exchange.interfaces.web.security.ApiPrincipal;
import com.example.exchange.interfaces.web.security.JwtAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    // Refresh sessions last longer than access JWTs and are the server-side logout/revocation handle.
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final AppUserRecordJpaRepository users;
    private final AuthRefreshSessionRecordJpaRepository sessions;
    private final AccountRepository accountRepository;
    private final PasswordHashService passwordHashService;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    @Autowired
    public AuthService(
            AppUserRecordJpaRepository users,
            AuthRefreshSessionRecordJpaRepository sessions,
            AccountRepository accountRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            ObjectMapper objectMapper
    ) {
        this(users, sessions, accountRepository, passwordHashService, jwtTokenService, objectMapper, Clock.systemUTC());
    }

    AuthService(
            AppUserRecordJpaRepository users,
            AuthRefreshSessionRecordJpaRepository sessions,
            AccountRepository accountRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.users = users;
        this.sessions = sessions;
        this.accountRepository = accountRepository;
        this.passwordHashService = passwordHashService;
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Registers a first-party exchange user and creates the matching internal account uid. */
    @Transactional
    public AuthResult register(String email, String password) {
        String normalizedEmail = AppUserRecord.normalizeEmail(email);
        requireEmail(normalizedEmail);
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("email already registered");
        }
        // saveAndFlush guarantees an IDENTITY-generated uid before creating the matching exchange account.
        AppUserRecord user = users.saveAndFlush(new AppUserRecord(normalizedEmail, passwordHashService.hash(password)));
        accountRepository.save(new Account(user.getId()));
        return issueAuth(user);
    }

    /** Authenticates local email/password credentials and creates a new refresh session. */
    @Transactional
    public AuthResult login(String email, String password) {
        String normalizedEmail = AppUserRecord.normalizeEmail(email);
        AppUserRecord user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
        if (!user.isActive() || !passwordHashService.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        return issueAuth(user);
    }

    /** Revokes the refresh session by token hash; missing tokens are treated as a no-op logout. */
    @Transactional
    public boolean logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        Optional<AuthRefreshSessionRecord> session = sessions.findByRefreshTokenHash(sha256Hex(refreshToken));
        if (session.isEmpty()) {
            return false;
        }
        session.get().revoke(Instant.now(clock));
        sessions.save(session.get());
        return true;
    }

    /** Resolves a stateless access JWT into the current active user for frontend session hydration. */
    public Optional<CurrentUser> currentUser(String bearerToken, long clockSkewSeconds) {
        Optional<ApiPrincipal> principal = new JwtAuthenticator(objectMapper, jwtTokenService.effectiveSecret(), clockSkewSeconds)
                .authenticate(bearerToken);
        return principal.flatMap(apiPrincipal -> parseUserId(apiPrincipal.subject()))
                .flatMap(users::findById)
                .filter(AppUserRecord::isActive)
                .map(this::toCurrentUser);
    }

    /** Builds the complete login payload and persists the refresh-token revocation handle. */
    private AuthResult issueAuth(AppUserRecord user) {
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueAccessToken(user);
        String refreshToken = randomToken();
        AuthRefreshSessionRecord session = new AuthRefreshSessionRecord(
                user.getId(),
                sha256Hex(refreshToken),
                Instant.now(clock).plus(REFRESH_TOKEN_TTL)
        );
        sessions.save(session);
        return new AuthResult(
                accessToken.token(),
                accessToken.expiresAt(),
                refreshToken,
                session.getExpiresAt(),
                toCurrentUser(user)
        );
    }

    /** Keeps API responses from exposing password hashes or persistence-only session fields. */
    private CurrentUser toCurrentUser(AppUserRecord user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getRoles(), user.getScopes());
    }

    /** Generates the raw refresh token returned once to the client; only its hash is stored. */
    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hashes refresh tokens so a database leak does not expose reusable logout/session credentials. */
    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    /** JWT subject must be the internal uid; non-numeric subjects are ignored. */
    private static Optional<Long> parseUserId(String subject) {
        try {
            return Optional.of(Long.parseLong(subject));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Minimal MVP validation; richer normalization and abuse checks belong in later hardening. */
    private static void requireEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("valid email is required");
        }
    }

    public record AuthResult(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            CurrentUser user
    ) {
    }

    public record CurrentUser(
            Long uid,
            String email,
            String roles,
            String scopes
    ) {
    }
}
