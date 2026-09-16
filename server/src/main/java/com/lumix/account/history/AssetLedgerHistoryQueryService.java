package com.lumix.account.history;

import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * authenticated owner 的 immutable asset history query service。
 *
 * <p>服務只由 session principal 帶入 userId，呼叫端不能指定其他 owner。多取一筆資料用以判斷是否仍有下一頁，
 * 不執行任何 ledger、projection 或帳戶寫入。</p>
 */
@Service
class AssetLedgerHistoryQueryService {
    static final int DEFAULT_LIMIT = 25;
    static final int MAX_LIMIT = 100;

    private final AssetLedgerHistoryQueryRepository repository;

    AssetLedgerHistoryQueryService(AssetLedgerHistoryQueryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    AssetLedgerHistoryPage listFor(AuthenticatedUser authenticatedUser, Optional<AssetLedgerHistoryCursor> before, Integer requestedLimit) {
        Objects.requireNonNull(authenticatedUser, "authenticatedUser must not be null");
        Objects.requireNonNull(before, "before must not be null");
        int limit = normalizeLimit(requestedLimit);
        List<AssetLedgerHistoryItem> fetched = List.copyOf(repository.findByOwnerUserId(authenticatedUser.userId(), before, limit + 1));
        List<AssetLedgerHistoryItem> items = fetched.size() > limit ? fetched.subList(0, limit) : fetched;
        AssetLedgerHistoryCursor nextCursor = fetched.size() > limit
            ? cursorFor(items.getLast())
            : null;
        return new AssetLedgerHistoryPage(items, nextCursor);
    }

    private static int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) return DEFAULT_LIMIT;
        if (requestedLimit < 1 || requestedLimit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return requestedLimit;
    }

    private static AssetLedgerHistoryCursor cursorFor(AssetLedgerHistoryItem item) {
        return new AssetLedgerHistoryCursor(item.postedAt(), item.entryId());
    }
}
