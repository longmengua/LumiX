package com.lumix.trading.core.futures.margin;

import com.lumix.account.AccountStatus;
import com.lumix.common.MoneyAmount;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Isolated initial-margin sufficiency 的 pure in-memory gate。
 *
 * 這個 gate 嚴格保持 stateless、deterministic、thread-safe by immutability，且只做 account / market / asset
 * 一致性檢查與 sandbox 容量比較，不接任何 I/O、clock、database 或外部服務。
 */
public final class IsolatedMarginCheckGate {

    /**
     * 針對單一 proposed position 進行 isolated initial-margin sufficiency 檢查。
     *
     * 比較公式使用 `availableMargin * leverage` 與 `quantity * entryPrice` 的 exact multiplication，
     * 因為在尚未定義 settlement asset precision 與 rounding policy 之前，乘法比較與正數條件下的
     * initial-margin sufficiency 判斷等價，且可避免除法帶來的 scale 與 rounding 爭議。
     */
    public IsolatedMarginCheckResult check(IsolatedMarginCheckRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        // 先擋掉目前模型理論上不該存在的非 isolated account，避免未來若 enum 擴張時靜默滑進錯誤路徑。
        if (!request.futuresAccount().marginMode().isIsolated()) {
            throw new IllegalArgumentException("futuresAccount marginMode must be ISOLATED");
        }

        // evaluation order 固定為 account status -> account identity -> market -> settlement asset -> notional calculation -> comparison，
        // 讓相同輸入永遠得到相同結果，也避免一致性失敗時產生誤導性的計算值。
        if (request.futuresAccount().status() != AccountStatus.ACTIVE) {
            return IsolatedMarginCheckResult.rejected(FuturesMarginCheckReason.ACCOUNT_NOT_ACTIVE);
        }
        if (!request.leverageConfig().futuresAccountId().equals(request.futuresAccount().accountId())) {
            return IsolatedMarginCheckResult.rejected(FuturesMarginCheckReason.ACCOUNT_MISMATCH);
        }
        if (!request.marketSymbol().equals(request.leverageConfig().marketSymbol())) {
            return IsolatedMarginCheckResult.rejected(FuturesMarginCheckReason.MARKET_MISMATCH);
        }
        if (!request.availableMarginAsset().equals(request.futuresAccount().settlementAsset())) {
            return IsolatedMarginCheckResult.rejected(FuturesMarginCheckReason.SETTLEMENT_ASSET_MISMATCH);
        }

        MoneyAmount requestedNotional = new MoneyAmount(
                request.quantity().value().multiply(request.entryPrice().value())
        );
        MoneyAmount marginSupportedNotional = new MoneyAmount(
                request.availableMargin().value().multiply(BigDecimal.valueOf(request.leverageConfig().leverage().multiplier()))
        );

        if (marginSupportedNotional.compareTo(requestedNotional) >= 0) {
            return IsolatedMarginCheckResult.approved(requestedNotional, marginSupportedNotional);
        }
        return IsolatedMarginCheckResult.insufficientMargin(requestedNotional, marginSupportedNotional);
    }
}
