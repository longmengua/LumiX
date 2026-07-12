package com.lumix.trading.core.spot.settlement;

import com.lumix.ledger.domain.LedgerDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox settlement 產生的 ledger posting candidate。
 *
 * 這份 candidate 只存在於設計 gate 與後續 review，不能直接 append 到 ledger。
 */
public record SpotSandboxLedgerPostingCommandCandidate(
        String requestId,
        String idempotencyKey,
        String businessReferenceType,
        String businessReferenceId,
        List<SpotSandboxLedgerPostingCommandLine> lines
) {

    /**
     * 建立不可變的 ledger posting candidate。
     *
     * 這裡只保留後續 review 所需的最小資訊，不代表 ledger posting runtime 已經執行。
     */
    public SpotSandboxLedgerPostingCommandCandidate {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(businessReferenceType, "businessReferenceType must not be null");
        Objects.requireNonNull(businessReferenceId, "businessReferenceId must not be null");
        Objects.requireNonNull(lines, "lines must not be null");
        lines = List.copyOf(lines);
    }

    /**
     * Ledger posting candidate 的單筆分錄。
     *
     * 這裡只描述方向與金額，不代表真正 ledger entry 已寫入。
     */
    public record SpotSandboxLedgerPostingCommandLine(
            String accountId,
            String assetSymbol,
            LedgerDirection direction,
            BigDecimal amount
    ) {

        /**
         * 建立不可變的候選分錄。
         *
         * 這裡只做最小 null 檢查，讓候選內容保持可追蹤。
         */
        public SpotSandboxLedgerPostingCommandLine {
            Objects.requireNonNull(accountId, "accountId must not be null");
            Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
            Objects.requireNonNull(direction, "direction must not be null");
            Objects.requireNonNull(amount, "amount must not be null");
        }
    }
}
