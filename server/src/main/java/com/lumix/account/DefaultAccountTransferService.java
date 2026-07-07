package com.lumix.account;

import com.lumix.common.BusinessException;
import com.lumix.common.ErrorCode;
import com.lumix.ledger.LedgerService;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Phase 9 的帳戶劃轉 stub。
 * 只做 request validation 與流程邊界保留，不做真實資產劃轉。
 */
public class DefaultAccountTransferService implements AccountTransferService {

    private static final Set<AccountType> SUPPORTED_ACCOUNT_TYPES =
            EnumSet.of(AccountType.SPOT, AccountType.FUTURES, AccountType.MARGIN);

    private final LedgerService ledgerService;

    public DefaultAccountTransferService(LedgerService ledgerService) {
        // LedgerService 先以依賴形式保留，之後 Phase 進一步接線時再補真正流程。
        this.ledgerService = Objects.requireNonNull(ledgerService, "ledgerService must not be null");
    }

    @Override
    public AccountTransferResult transfer(AccountTransferRequest request) {
        // 先做輸入驗證，避免把明顯非法的請求送入後續高風險流程。
        Objects.requireNonNull(request, "request must not be null");
        validateRequest(request);

        // 這裡刻意不呼叫任何真實 ledger 寫入流程，避免誤解為可上線實作。
        // TODO(HUMAN_REVIEW_REQUIRED): 目前刻意不接真實 reserve/postJournal/commit 流程，避免把 stub 誤認為可上線的資金路徑。
        return new AccountTransferResult(
                request.requestId(),
                AccountTransferStatus.PENDING_LEDGER_REVIEW,
                "Transfer request validated. Ledger execution is intentionally not implemented in Phase 9.",
                null
        );
    }

    private void validateRequest(AccountTransferRequest request) {
        // 劃轉金額必須為正數，零或負數都視為非法輸入。
        if (!request.amount().isPositive()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Transfer amount must be greater than zero");
        }
        // 同帳戶類型對倒沒有業務意義，Phase 9 直接拒絕。
        if (request.fromAccountType() == request.toAccountType()) {
            throw new BusinessException(
                    ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED,
                    "fromAccountType and toAccountType must be different"
            );
        }
        // 只接受 Phase 9 已定義的三種帳戶域。
        validateAccountType(request.fromAccountType(), "fromAccountType");
        validateAccountType(request.toAccountType(), "toAccountType");
    }

    private void validateAccountType(AccountType accountType, String fieldName) {
        // 目前只允許現貨、合約、槓桿三個帳戶域，禁止拿 generic account 當萬用桶。
        if (!SUPPORTED_ACCOUNT_TYPES.contains(accountType)) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_ACCOUNT_TYPE,
                    fieldName + " must be one of SPOT, FUTURES, or MARGIN"
            );
        }
    }
}
