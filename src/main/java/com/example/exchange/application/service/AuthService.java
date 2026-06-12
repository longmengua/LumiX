/*
 * File purpose: Local first-party registration, login, logout, and current-user lookup.
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.AppUserRecord;
import com.example.exchange.domain.model.entity.AuthRefreshSessionRecord;
import com.example.exchange.domain.model.entity.CustomerRegistrationRecord;
import com.example.exchange.domain.model.entity.CustomerVerificationCodeRecord;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.jpa.AppUserRecordJpaRepository;
import com.example.exchange.domain.repository.jpa.AuthRefreshSessionRecordJpaRepository;
import com.example.exchange.domain.repository.jpa.CustomerRegistrationRecordJpaRepository;
import com.example.exchange.domain.repository.jpa.CustomerVerificationCodeRecordJpaRepository;
import com.example.exchange.infra.config.CustomerAuthProperties;
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
    private static final Duration DEFAULT_EMAIL_TOKEN_TTL = Duration.ofMinutes(30);

    private final AppUserRecordJpaRepository users;
    private final CustomerRegistrationRecordJpaRepository registrations;
    private final CustomerVerificationCodeRecordJpaRepository verificationCodes;
    private final AuthRefreshSessionRecordJpaRepository sessions;
    private final AccountRepository accountRepository;
    private final PasswordHashService passwordHashService;
    private final JwtTokenService jwtTokenService;
    private final CustomerAuthProperties customerAuthProperties;
    private final HumanVerificationService humanVerificationService;
    private final EmailVerificationNotifier emailVerificationNotifier;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    @Autowired
    public AuthService(
            AppUserRecordJpaRepository users,
            CustomerRegistrationRecordJpaRepository registrations,
            CustomerVerificationCodeRecordJpaRepository verificationCodes,
            AuthRefreshSessionRecordJpaRepository sessions,
            AccountRepository accountRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            CustomerAuthProperties customerAuthProperties,
            HumanVerificationService humanVerificationService,
            EmailVerificationNotifier emailVerificationNotifier,
            ObjectMapper objectMapper
    ) {
        this(users, registrations, verificationCodes, sessions, accountRepository, passwordHashService, jwtTokenService,
                customerAuthProperties, humanVerificationService, emailVerificationNotifier, objectMapper, Clock.systemUTC());
    }

    AuthService(
            AppUserRecordJpaRepository users,
            CustomerRegistrationRecordJpaRepository registrations,
            CustomerVerificationCodeRecordJpaRepository verificationCodes,
            AuthRefreshSessionRecordJpaRepository sessions,
            AccountRepository accountRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            CustomerAuthProperties customerAuthProperties,
            HumanVerificationService humanVerificationService,
            EmailVerificationNotifier emailVerificationNotifier,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.users = users;
        this.registrations = registrations;
        this.verificationCodes = verificationCodes;
        this.sessions = sessions;
        this.accountRepository = accountRepository;
        this.passwordHashService = passwordHashService;
        this.jwtTokenService = jwtTokenService;
        this.customerAuthProperties = customerAuthProperties;
        this.humanVerificationService = humanVerificationService;
        this.emailVerificationNotifier = emailVerificationNotifier;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Registers a first-party exchange user, runs anti-bot verification, and starts email verification when enabled. */
    @Transactional
    public RegistrationResult register(String email, String password, String humanVerificationToken) {
        humanVerificationService.verifyRegistration(humanVerificationToken);
        String normalizedEmail = AppUserRecord.normalizeEmail(email);
        requireEmail(normalizedEmail);
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("email already registered");
        }
        if (emailVerificationEnabled()) {
            Instant now = Instant.now(clock);
            Optional<CustomerRegistrationRecord> existingPending = registrations
                    .findFirstByEmailAndStatusOrderByCreatedAtDesc(normalizedEmail, CustomerRegistrationRecord.STATUS_PENDING);
            if (existingPending.isPresent() && !existingPending.get().isExpired(now)) {
                throw new IllegalStateException("registration verification already pending");
            }
            existingPending.filter(record -> record.isExpired(now)).ifPresent(record -> {
                record.expire();
                registrations.save(record);
            });
            String token = randomToken();
            String code = verificationCode();
            Instant expiresAt = now.plus(registrationTtl());
            CustomerRegistrationRecord registration = registrations.save(new CustomerRegistrationRecord(
                    normalizedEmail,
                    passwordHashService.hash(password),
                    sha256Hex(token),
                    expiresAt
            ));
            // Verification codes live in their own table so operators can later resend or inspect code state by email/account.
            verificationCodes.save(new CustomerVerificationCodeRecord(
                    normalizedEmail,
                    null,
                    registration.getId(),
                    verificationCodeHash(normalizedEmail, code),
                    expiresAt
            ));
            String verificationUrl = verificationUrl(token);
            emailVerificationNotifier.sendVerification(registration.getEmail(), verificationUrl, code, expiresAt);
            return new RegistrationResult(
                    null,
                    registration.getEmail(),
                    true,
                    customerAuthProperties.getEmailVerification().isReturnVerificationUrl() ? verificationUrl : null,
                    expiresAt
            );
        }
        // saveAndFlush guarantees an IDENTITY-generated uid before creating the matching exchange account.
        AppUserRecord user = users.saveAndFlush(new AppUserRecord(normalizedEmail, passwordHashService.hash(password)));
        user.verifyEmail(Instant.now(clock));
        users.save(user);
        accountRepository.save(new Account(user.getId()));
        return new RegistrationResult(
                user.getId(),
                user.getEmail(),
                false,
                null,
                null
        );
    }

    /** Authenticates local email/password credentials and creates a new refresh session. */
    @Transactional
    public AuthResult login(String email, String password) {
        String normalizedEmail = AppUserRecord.normalizeEmail(email);
        AppUserRecord user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
        if (!passwordHashService.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        if (emailVerificationEnabled() && user.isPendingEmailVerification() && !user.isEmailVerified()) {
            throw new IllegalStateException("email verification required");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("invalid credentials");
        }
        if (emailVerificationEnabled() && !user.isEmailVerified()) {
            throw new IllegalStateException("email verification required");
        }
        return issueAuth(user);
    }

    /** Completes a pending registration when the raw email token matches and has not expired. */
    @Transactional
    public CurrentUser verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("email verification token is required");
        }
        CustomerRegistrationRecord registration = registrations
                .findByVerificationTokenHashAndStatus(sha256Hex(token), CustomerRegistrationRecord.STATUS_PENDING)
                .orElseThrow(() -> new IllegalArgumentException("email verification token is invalid"));
        return completeRegistration(registration, latestPendingCode(registration.getEmail()).orElse(null));
    }

    /** Completes a pending registration from the six-digit code typed into the registration verification step. */
    @Transactional
    public CurrentUser verifyEmailCode(String email, String code) {
        String normalizedEmail = AppUserRecord.normalizeEmail(email);
        if (normalizedEmail.isBlank() || code == null || code.isBlank()) {
            throw new IllegalArgumentException("email and verification code are required");
        }
        CustomerRegistrationRecord registration = registrations
                .findFirstByEmailAndStatusOrderByCreatedAtDesc(normalizedEmail, CustomerRegistrationRecord.STATUS_PENDING)
                .orElseThrow(() -> new IllegalArgumentException("registration verification is invalid"));
        CustomerVerificationCodeRecord verificationCode = latestPendingCode(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("registration verification code is invalid"));
        Instant now = Instant.now(clock);
        if (verificationCode.isExpired(now)) {
            verificationCode.expire();
            verificationCodes.save(verificationCode);
            throw new IllegalArgumentException("registration verification is expired");
        }
        String expectedHash = verificationCodeHash(normalizedEmail, code);
        if (!expectedHash.equals(verificationCode.getCodeHash())) {
            throw new IllegalArgumentException("registration verification code is invalid");
        }
        return completeRegistration(registration, verificationCode);
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
                .filter(user -> !emailVerificationEnabled() || user.isEmailVerified())
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

    private boolean emailVerificationEnabled() {
        return customerAuthProperties.getEmailVerification().isEnabled();
    }

    private Duration emailTokenTtl() {
        int minutes = customerAuthProperties.getEmailVerification().getTokenTtlMinutes();
        return minutes > 0 ? Duration.ofMinutes(minutes) : DEFAULT_EMAIL_TOKEN_TTL;
    }

    private Duration registrationTtl() {
        int hours = customerAuthProperties.getEmailVerification().getRegistrationTtlHours();
        return hours > 0 ? Duration.ofHours(hours) : Duration.ofHours(24);
    }

    private String verificationUrl(String token) {
        String baseUrl = customerAuthProperties.getEmailVerification().getPublicBaseUrl();
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "verifyEmailToken=" + token;
    }

    private CurrentUser completeRegistration(
            CustomerRegistrationRecord registration,
            CustomerVerificationCodeRecord verificationCode
    ) {
        Instant now = Instant.now(clock);
        if (registration.isExpired(now)) {
            registration.expire();
            registrations.save(registration);
            if (verificationCode != null) {
                verificationCode.expire();
                verificationCodes.save(verificationCode);
            }
            throw new IllegalArgumentException("registration verification is expired");
        }
        if (users.existsByEmail(registration.getEmail())) {
            registration.expire();
            registrations.save(registration);
            if (verificationCode != null) {
                verificationCode.expire();
                verificationCodes.save(verificationCode);
            }
            throw new IllegalArgumentException("email already registered");
        }
        AppUserRecord user = users.saveAndFlush(new AppUserRecord(registration.getEmail(), registration.getPasswordHash()));
        user.verifyEmail(now);
        users.save(user);
        accountRepository.save(new Account(user.getId()));
        registration.verify(now);
        registrations.save(registration);
        if (verificationCode != null) {
            verificationCode.verify(now, user.getId());
            verificationCodes.save(verificationCode);
        }
        return toCurrentUser(user);
    }

    private String verificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private Optional<CustomerVerificationCodeRecord> latestPendingCode(String normalizedEmail) {
        return verificationCodes.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                normalizedEmail,
                CustomerVerificationCodeRecord.STATUS_PENDING
        );
    }

    private String verificationCodeHash(String normalizedEmail, String code) {
        return sha256Hex(normalizedEmail + ":" + code.trim());
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

    public record RegistrationResult(
            Long uid,
            String email,
            boolean emailVerificationRequired,
            String verificationUrl,
            Instant expiresAt
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
