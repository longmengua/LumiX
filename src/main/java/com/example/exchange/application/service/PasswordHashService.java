/*
 * File purpose: PBKDF2 password hashing for local exchange authentication.
 */
package com.example.exchange.application.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordHashService {

    // PBKDF2 keeps the MVP dependency-light while still avoiding reversible or fast password storage.
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Hashes a raw registration password with a per-password random salt. */
    public String hash(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        return "pbkdf2_sha256$" + ITERATIONS + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /** Verifies a login password using constant-time comparison against the stored hash bytes. */
    public boolean matches(String password, String encodedHash) {
        if (password == null || encodedHash == null || encodedHash.isBlank()) {
            return false;
        }
        String[] parts = encodedHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** PBKDF2 details are centralized so future iteration upgrades have one implementation point. */
    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("password hashing unavailable", ex);
        }
    }
}
