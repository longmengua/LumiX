/*
 * 檔案用途：測試 Polymarket session signer lifecycle guard。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.entity.PredictionSessionRecord;
import com.example.exchange.domain.repository.jpa.PredictionSessionRecordRepository;
import com.example.exchange.interfaces.web.dto.SessionConfirmRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolymarketSessionServiceTest {

    private static final String USER =
            "0x0000000000000000000000000000000000000001";

    @Test
    @DisplayName("revoke all 會撤銷 PENDING 與 ACTIVE session，避免已簽發 signer 之後被 confirm")
    /**
     * 流程：同一 wallet 有一個 PENDING signer 和一個 ACTIVE signer，使用者執行 revoke all。
     * 期望：兩者都轉 REVOKED；PENDING signer 後續不能再 confirm 成 ACTIVE。
     */
    void revokeAllRevokesPendingAndActiveSessions() {
        InMemorySessionRepository repository =
                new InMemorySessionRepository(
                        session("pending-1", "PENDING", Instant.now().plusSeconds(60).getEpochSecond()),
                        session("active-1", "ACTIVE", Instant.now().plusSeconds(60).getEpochSecond())
                );
        PolymarketSessionService service =
                new PolymarketSessionService(repository.proxy());

        assertThat(service.revokeAllSessions(USER))
                .isEqualTo("all sessions revoked");

        assertThat(repository.record("pending-1").getStatus())
                .isEqualTo("REVOKED");
        assertThat(repository.record("pending-1").getRevokedReason())
                .isEqualTo("USER_REVOKED_ALL");
        assertThat(repository.record("active-1").getStatus())
                .isEqualTo("REVOKED");

        SessionConfirmRequest confirm =
                new SessionConfirmRequest();
        confirm.setSessionId("pending-1");
        confirm.setUserAddress(USER);
        confirm.setSignature("0x" + "00".repeat(65));

        assertThatThrownBy(() -> service.confirmSession(confirm))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session status invalid: REVOKED");
    }

    @Test
    @DisplayName("consume limit 前會重新檢查 session 狀態與過期時間")
    /**
     * 流程：下單流程拿到的 session 已過期，直接呼叫 limit consume。
     * 期望：服務會把 session 標記 EXPIRED 並拒絕扣用量，避免 stale record 繞過生命周期保護。
     */
    void consumeLimitRejectsExpiredSessionAndMarksItExpired() {
        PredictionSessionRecord expired =
                session("active-1", "ACTIVE", Instant.now().minusSeconds(1).getEpochSecond());
        InMemorySessionRepository repository =
                new InMemorySessionRepository(expired);
        PolymarketSessionService service =
                new PolymarketSessionService(repository.proxy());

        assertThatThrownBy(() -> service.assertAndConsumeLimit(expired, new BigDecimal("10")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session expired");

        assertThat(expired.getStatus())
                .isEqualTo("EXPIRED");
        assertThat(expired.getDailyUsedUsdt())
                .isZero();
    }

    private static PredictionSessionRecord session(
            String sessionId,
            String status,
            long expiresAt
    ) {
        return PredictionSessionRecord.builder()
                .sessionId(sessionId)
                .userAddress(USER)
                .sessionSignerAddress("0x0000000000000000000000000000000000000002")
                .sessionPrivateKey("0x0123456789012345678901234567890123456789012345678901234567890123")
                .typedData("{}")
                .status(status)
                .expiresAt(expiresAt)
                .dailyUsedUsdt(BigDecimal.ZERO)
                .dailyResetDate(LocalDate.now().toString())
                .build();
    }

    private static class InMemorySessionRepository {
        private final List<PredictionSessionRecord> records =
                new ArrayList<>();

        private InMemorySessionRepository(PredictionSessionRecord... records) {
            this.records.addAll(List.of(records));
        }

        private PredictionSessionRecordRepository proxy() {
            return (PredictionSessionRecordRepository) Proxy.newProxyInstance(
                    PredictionSessionRecordRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionSessionRecordRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findBySessionId" -> Optional.ofNullable(record((String) args[0]));
                        case "findByUserAddressOrderByIdDesc" -> records.stream()
                                .filter(record -> record.getUserAddress().equalsIgnoreCase((String) args[0]))
                                .toList();
                        case "save" -> args[0];
                        case "saveAll" -> args[0];
                        case "toString" -> "InMemorySessionRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private PredictionSessionRecord record(String sessionId) {
            return records.stream()
                    .filter(record -> record.getSessionId().equals(sessionId))
                    .findFirst()
                    .orElse(null);
        }
    }
}
