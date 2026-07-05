package com.lumix.account;

import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;

import java.util.Objects;

/**
 * 帳戶劃轉請求。
 * 這是 Phase 9 的輸入模型，只負責攜帶必要欄位與基本正規化。
 */
public record AccountTransferRequest(
        RequestId requestId,
        UserId userId,
        AccountType fromAccountType,
        AccountType toAccountType,
        AssetSymbol asset,
        MoneyAmount amount,
        String clientReferenceId
) {

    public AccountTransferRequest {
        // 劃轉流程會跨帳本與冪等層，所有核心標識欄位都必須存在。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(fromAccountType, "fromAccountType must not be null");
        Objects.requireNonNull(toAccountType, "toAccountType must not be null");
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        // clientReferenceId 是選填欄位，只做 trim 與空字串歸零，避免污染下游記錄。
        if (clientReferenceId != null) {
            clientReferenceId = clientReferenceId.trim();
            if (clientReferenceId.isEmpty()) {
                clientReferenceId = null;
            }
        }
    }
}
