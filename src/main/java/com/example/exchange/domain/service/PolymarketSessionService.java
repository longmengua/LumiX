/*
 * 檔案用途：領域服務，封裝撮合、風控、Polymarket 同步與交易規則。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.entity.PredictionSessionRecord;
import com.example.exchange.domain.repository.jpa.PredictionSessionRecordRepository;
import com.example.exchange.interfaces.web.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polymarket Session Signer Service。
 *
 * 正式架構：
 *
 * Deposit Wallet：
 * - 使用者 MetaMask
 * - 真正持有資產
 * - approve exchange
 *
 * Session Signer：
 * - 後端生成 ephemeral wallet
 * - 只負責 sign CLOB order
 * - 不持有資產
 * - 可 revoke
 * - 可 expire
 *
 * 流程：
 * 1. init session
 * 2. eth_signTypedData_v4
 * 3. confirm session
 * 4. ACTIVE
 * 5. place order
 * 6. revoke / expire
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketSessionService {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    /**
     * Session TTL。
     *
     * TODO:
     * 正式環境建議 config 化。
     */
    private static final long SESSION_TTL_SECONDS =
            86_400L * 7;

    /**
     * EIP712 domain。
     */
    private static final String EIP712_DOMAIN_NAME =
            "Prediction Market Session";

    private static final String EIP712_DOMAIN_VERSION =
            "1";

    private static final Integer CHAIN_ID =
            137;

    /**
     * TODO:
     * 如果後續有自己的 SessionManager contract，
     * verifyingContract 要改正式地址。
     */
    private static final String VERIFYING_CONTRACT =
            "0x0000000000000000000000000000000000000000";

    private final PredictionSessionRecordRepository
            sessionRecordRepository;

    /**
     * 初始化 session signer。
     */
    public SessionInitResponse initSession(
            SessionInitRequest request
    ) {
        validateInitRequest(request);

        try {

            /**
             * TODO:
             * 後續如果限制：
             * 一個 wallet 只能一個 ACTIVE session，
             * 這裡可先 revoke 舊 session。
             */

            String sessionId =
                    UUID.randomUUID().toString();

            ECKeyPair keyPair =
                    Keys.createEcKeyPair();

            String sessionPrivateKey =
                    "0x" + keyPair.getPrivateKey().toString(16);

            String sessionSignerAddress =
                    "0x" + Keys.getAddress(keyPair);

            long issuedAt =
                    Instant.now().getEpochSecond();

            long expiresAt =
                    issuedAt + SESSION_TTL_SECONDS;

            Map<String, Object> typedData =
                    buildSessionTypedData(
                            request.getUserAddress(),
                            sessionSignerAddress,
                            sessionId,
                            issuedAt,
                            expiresAt
                    );

            PredictionSessionRecord record =
                    PredictionSessionRecord.builder()
                            .sessionId(sessionId)
                            .userAddress(request.getUserAddress())
                            .sessionSignerAddress(sessionSignerAddress)

                            /**
                             * TODO:
                             * 正式環境必須：
                             * AES/KMS/Vault encrypt。
                             */
                            .sessionPrivateKey(sessionPrivateKey)

                            .typedData(
                                    OBJECT_MAPPER.writeValueAsString(typedData)
                            )

                            .status("PENDING")
                            .issuedAt(issuedAt)
                            .expiresAt(expiresAt)
                            .createdAt(Instant.now().toString())
                            .maxOrderUsdt(resolveMaxOrderUsdt(request))
                            .dailyLimitUsdt(resolveDailyLimitUsdt(request))
                            .dailyUsedUsdt(BigDecimal.ZERO)
                            .dailyResetDate(LocalDate.now().toString())
                            .build();

            sessionRecordRepository.save(record);

            log.info(
                    "[Session] Init success. sessionId={}, userAddress={}, sessionSignerAddress={}, expiresAt={}",
                    sessionId,
                    request.getUserAddress(),
                    sessionSignerAddress,
                    expiresAt
            );

            return SessionInitResponse.builder()
                    .sessionId(sessionId)
                    .userAddress(request.getUserAddress())
                    .sessionSignerAddress(sessionSignerAddress)
                    .typedData(typedData)
                    .status("PENDING")
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .build();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Init Polymarket session signer failed",
                    e
            );
        }
    }

    /**
     * Confirm session signer。
     */
    public SessionConfirmResponse confirmSession(
            SessionConfirmRequest request
    ) {
        validateConfirmRequest(request);

        PredictionSessionRecord record =
                sessionRecordRepository
                        .findBySessionId(request.getSessionId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "session not found"
                                )
                        );

        if (!record.getUserAddress()
                .equalsIgnoreCase(request.getUserAddress())) {

            throw new IllegalArgumentException(
                    "userAddress mismatch"
            );
        }

        long now =
                Instant.now().getEpochSecond();

        if (record.getExpiresAt() != null
                && now > record.getExpiresAt()) {

            record.setStatus("EXPIRED");

            sessionRecordRepository.save(record);

            throw new IllegalStateException(
                    "session expired"
            );
        }

        if (!"PENDING".equalsIgnoreCase(record.getStatus())) {

            throw new IllegalStateException(
                    "session status invalid: "
                            + record.getStatus()
            );
        }

        try {

            Map<String, Object> typedData =
                    OBJECT_MAPPER.readValue(
                            record.getTypedData(),
                            Map.class
                    );

            String recoveredAddress =
                    recoverTypedDataSigner(
                            typedData,
                            request.getSignature()
                    );

            if (!recoveredAddress.equalsIgnoreCase(
                    record.getUserAddress())) {

                throw new IllegalStateException(
                        "invalid signature. recovered="
                                + recoveredAddress
                                + ", expected="
                                + record.getUserAddress()
                );
            }

            record.setSignature(
                    request.getSignature()
            );

            record.setStatus("ACTIVE");

            record.setConfirmedAt(
                    Instant.now().toString()
            );

            record.setLastUsedAt(
                    Instant.now().toString()
            );

            sessionRecordRepository.save(record);

            log.info(
                    "[Session] Confirm success. sessionId={}, userAddress={}",
                    record.getSessionId(),
                    record.getUserAddress()
            );

            return SessionConfirmResponse.builder()
                    .sessionId(record.getSessionId())
                    .userAddress(record.getUserAddress())
                    .sessionSignerAddress(record.getSessionSignerAddress())
                    .status(record.getStatus())
                    .build();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Confirm Polymarket session signer failed",
                    e
            );
        }
    }

    /**
     * 查 ACTIVE session。
     *
     * placeOrder 前使用。
     */
    public PredictionSessionRecord getActiveSession(
            String sessionId
    ) {

        PredictionSessionRecord record =
                sessionRecordRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "session not found"
                                )
                        );

        if (!"ACTIVE".equalsIgnoreCase(
                record.getStatus())) {

            throw new IllegalStateException(
                    "session is not active"
            );
        }

        long now =
                Instant.now().getEpochSecond();

        if (record.getExpiresAt() != null
                && now > record.getExpiresAt()) {

            record.setStatus("EXPIRED");

            sessionRecordRepository.save(record);

            throw new IllegalStateException(
                    "session expired"
            );
        }

        /**
         * 更新最後使用時間。
         */
        record.setLastUsedAt(
                Instant.now().toString()
        );

        sessionRecordRepository.save(record);

        return record;
    }

    /**
     * Session list。
     */
    public List<SessionListResponse> listSessions(
            String userAddress
    ) {

        return sessionRecordRepository
                .findByUserAddressOrderByIdDesc(
                        userAddress
                )
                .stream()
                .map(session ->
                        SessionListResponse.builder()
                                .sessionId(session.getSessionId())
                                .userAddress(session.getUserAddress())
                                .sessionSignerAddress(session.getSessionSignerAddress())
                                .status(session.getStatus())
                                .issuedAt(session.getIssuedAt())
                                .expiresAt(session.getExpiresAt())
                                .createdAt(session.getCreatedAt())
                                .confirmedAt(session.getConfirmedAt())
                                .lastUsedAt(session.getLastUsedAt())
                                .maxOrderUsdt(session.getMaxOrderUsdt())
                                .dailyLimitUsdt(session.getDailyLimitUsdt())
                                .dailyUsedUsdt(session.getDailyUsedUsdt())
                                .dailyResetDate(session.getDailyResetDate())
                                .build()
                )
                .toList();
    }

    /**
     * Revoke single session。
     */
    public String revokeSession(
            SessionRevokeRequest request
    ) {

        PredictionSessionRecord session =
                sessionRecordRepository
                        .findBySessionId(request.getSessionId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "session not found"
                                )
                        );

        if (!session.getUserAddress()
                .equalsIgnoreCase(request.getUserAddress())) {

            throw new IllegalStateException(
                    "wallet owner mismatch"
            );
        }

        session.setStatus("REVOKED");
        session.setRevokedReason("USER_REVOKED");

        session.setRevokedAt(
                Instant.now().toString()
        );

        sessionRecordRepository.save(session);

        log.info(
                "[Session] Revoke success. sessionId={}, userAddress={}",
                session.getSessionId(),
                session.getUserAddress()
        );

        return "session revoked";
    }

    /**
     * Revoke all sessions。
     */
    public String revokeAllSessions(
            String userAddress
    ) {

        List<PredictionSessionRecord> sessions =
                sessionRecordRepository
                        .findByUserAddressOrderByIdDesc(
                                userAddress
                        );

        for (PredictionSessionRecord session : sessions) {

            if (!isUsableSession(session)) {
                continue;
            }

            session.setStatus("REVOKED");
            session.setRevokedReason("USER_REVOKED_ALL");

            session.setRevokedAt(
                    Instant.now().toString()
            );
        }

        sessionRecordRepository.saveAll(sessions);

        log.info(
                "[Session] Revoke all success. userAddress={}",
                userAddress
        );

        return "all sessions revoked";
    }

    /**
     * Expire old sessions。
     *
     * TODO:
     * 正式環境建議 scheduler 定時執行。
     */
    public int expireSessions() {

        long now =
                Instant.now().getEpochSecond();

        List<PredictionSessionRecord> expired =
                sessionRecordRepository
                        .findByStatusAndExpiresAtLessThan(
                                "ACTIVE",
                                now
                        );

        for (PredictionSessionRecord session : expired) {

            session.setStatus("EXPIRED");
        }

        sessionRecordRepository.saveAll(expired);
        return expired.size();
    }

    public void assertAndConsumeLimit(
            PredictionSessionRecord session,
            BigDecimal usdtAmount
    ) {
        if (session == null || usdtAmount == null) {
            return;
        }

        assertUsableSession(session);

        resetDailyUsageIfNeeded(session);

        if (session.getMaxOrderUsdt() != null
                && usdtAmount.compareTo(session.getMaxOrderUsdt()) > 0) {
            log.warn(
                    "[Session] Abnormal usage rejected. reason=MAX_ORDER_LIMIT sessionId={}, userAddress={}, requested={}, max={}",
                    session.getSessionId(),
                    session.getUserAddress(),
                    usdtAmount,
                    session.getMaxOrderUsdt()
            );
            throw new IllegalStateException(
                    "session max order limit exceeded. max="
                            + session.getMaxOrderUsdt()
                            + ", requested="
                            + usdtAmount
            );
        }

        BigDecimal dailyUsed =
                session.getDailyUsedUsdt() == null
                        ? BigDecimal.ZERO
                        : session.getDailyUsedUsdt();

        if (session.getDailyLimitUsdt() != null
                && dailyUsed.add(usdtAmount).compareTo(session.getDailyLimitUsdt()) > 0) {
            log.warn(
                    "[Session] Abnormal usage rejected. reason=DAILY_LIMIT sessionId={}, userAddress={}, requested={}, used={}, limit={}",
                    session.getSessionId(),
                    session.getUserAddress(),
                    usdtAmount,
                    dailyUsed,
                    session.getDailyLimitUsdt()
            );
            throw new IllegalStateException(
                    "session daily limit exceeded. limit="
                            + session.getDailyLimitUsdt()
                            + ", used="
                            + dailyUsed
                            + ", requested="
                            + usdtAmount
            );
        }

        session.setDailyUsedUsdt(dailyUsed.add(usdtAmount));
        session.setLastUsedAt(Instant.now().toString());
        sessionRecordRepository.save(session);
    }

    private void assertUsableSession(PredictionSessionRecord session) {
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            log.warn(
                    "[Session] Usage rejected for inactive session. sessionId={}, userAddress={}, status={}",
                    session.getSessionId(),
                    session.getUserAddress(),
                    session.getStatus()
            );
            throw new IllegalStateException(
                    "session is not active"
            );
        }

        long now =
                Instant.now().getEpochSecond();

        if (session.getExpiresAt() != null
                && now > session.getExpiresAt()) {

            session.setStatus("EXPIRED");
            sessionRecordRepository.save(session);

            log.warn(
                    "[Session] Usage rejected for expired session. sessionId={}, userAddress={}",
                    session.getSessionId(),
                    session.getUserAddress()
            );

            throw new IllegalStateException(
                    "session expired"
            );
        }
    }

    private boolean isUsableSession(PredictionSessionRecord session) {
        return session != null
                && ("ACTIVE".equalsIgnoreCase(session.getStatus())
                || "PENDING".equalsIgnoreCase(session.getStatus()));
    }

    private void resetDailyUsageIfNeeded(PredictionSessionRecord session) {
        String today =
                LocalDate.now().toString();

        if (!today.equals(session.getDailyResetDate())) {
            session.setDailyResetDate(today);
            session.setDailyUsedUsdt(BigDecimal.ZERO);
        }
    }

    private BigDecimal resolveMaxOrderUsdt(SessionInitRequest request) {
        if (request.getMaxOrderUsdt() != null
                && request.getMaxOrderUsdt().signum() > 0) {
            return request.getMaxOrderUsdt();
        }

        return new BigDecimal("100");
    }

    private BigDecimal resolveDailyLimitUsdt(SessionInitRequest request) {
        if (request.getDailyLimitUsdt() != null
                && request.getDailyLimitUsdt().signum() > 0) {
            return request.getDailyLimitUsdt();
        }

        return new BigDecimal("1000");
    }

    /**
     * 建立 EIP712 typedData。
     */
    private Map<String, Object> buildSessionTypedData(
            String userAddress,
            String sessionSignerAddress,
            String sessionId,
            Long issuedAt,
            Long expiresAt
    ) {

        Map<String, Object> domain =
                new LinkedHashMap<>();

        domain.put("name", EIP712_DOMAIN_NAME);
        domain.put("version", EIP712_DOMAIN_VERSION);
        domain.put("chainId", CHAIN_ID);
        domain.put("verifyingContract", VERIFYING_CONTRACT);

        Map<String, Object> types =
                new LinkedHashMap<>();

        types.put(
                "EIP712Domain",
                List.of(
                        typedField("name", "string"),
                        typedField("version", "string"),
                        typedField("chainId", "uint256"),
                        typedField("verifyingContract", "address")
                )
        );

        types.put(
                "SessionAuthorization",
                List.of(
                        typedField("userAddress", "address"),
                        typedField("sessionSigner", "address"),
                        typedField("sessionId", "string"),
                        typedField("permission", "string"),
                        typedField("issuedAt", "uint256"),
                        typedField("expiresAt", "uint256")
                )
        );

        Map<String, Object> message =
                new LinkedHashMap<>();

        message.put("userAddress", userAddress);
        message.put("sessionSigner", sessionSignerAddress);
        message.put("sessionId", sessionId);

        /**
         * TODO:
         * 後續可細拆權限：
         * - PLACE_ORDER
         * - CANCEL_ORDER
         * - VIEW_POSITION
         */
        message.put(
                "permission",
                "PLACE_ORDER_CANCEL_ORDER_ONLY"
        );

        message.put("issuedAt", issuedAt);
        message.put("expiresAt", expiresAt);

        Map<String, Object> typedData =
                new LinkedHashMap<>();

        typedData.put("types", types);
        typedData.put("primaryType", "SessionAuthorization");
        typedData.put("domain", domain);
        typedData.put("message", message);

        return typedData;
    }

    private Map<String, String> typedField(
            String name,
            String type
    ) {

        Map<String, String> field =
                new LinkedHashMap<>();

        field.put("name", name);
        field.put("type", type);

        return field;
    }

    /**
     * Recover EIP712 signer。
     */
    private String recoverTypedDataSigner(
            Map<String, Object> typedData,
            String signature
    ) throws Exception {

        String json =
                OBJECT_MAPPER.writeValueAsString(
                        typedData
                );

        StructuredDataEncoder encoder =
                new StructuredDataEncoder(json);

        byte[] hash =
                encoder.hashStructuredData();

        Sign.SignatureData signatureData =
                parseSignature(signature);

        BigInteger publicKey =
                Sign.signedMessageHashToKey(
                        hash,
                        signatureData
                );

        return "0x" + Keys.getAddress(publicKey);
    }

    /**
     * Parse Ethereum signature。
     */
    private Sign.SignatureData parseSignature(
            String signature
    ) {

        byte[] signatureBytes =
                Numeric.hexStringToByteArray(signature);

        if (signatureBytes.length != 65) {

            throw new IllegalArgumentException(
                    "Invalid signature length: "
                            + signatureBytes.length
            );
        }

        byte v =
                signatureBytes[64];

        if (v < 27) {
            v += 27;
        }

        byte[] r =
                new byte[32];

        byte[] s =
                new byte[32];

        System.arraycopy(
                signatureBytes,
                0,
                r,
                0,
                32
        );

        System.arraycopy(
                signatureBytes,
                32,
                s,
                0,
                32
        );

        return new Sign.SignatureData(
                v,
                r,
                s
        );
    }

    private void validateInitRequest(
            SessionInitRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "request is required"
            );
        }

        if (request.getUserAddress() == null
                || request.getUserAddress().isBlank()) {

            throw new IllegalArgumentException(
                    "userAddress is required"
            );
        }

        if (!request.getUserAddress().startsWith("0x")) {

            throw new IllegalArgumentException(
                    "invalid userAddress"
            );
        }
    }

    private void validateConfirmRequest(
            SessionConfirmRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "request is required"
            );
        }

        if (request.getSessionId() == null
                || request.getSessionId().isBlank()) {

            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }

        if (request.getUserAddress() == null
                || request.getUserAddress().isBlank()) {

            throw new IllegalArgumentException(
                    "userAddress is required"
            );
        }

        if (request.getSignature() == null
                || request.getSignature().isBlank()) {

            throw new IllegalArgumentException(
                    "signature is required"
            );
        }
    }
}
