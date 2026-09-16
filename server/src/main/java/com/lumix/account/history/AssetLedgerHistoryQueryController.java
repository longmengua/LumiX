package com.lumix.account.history;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登入者自己的 immutable asset history API。
 *
 * <p>cursor 與 limit 只控制讀取位置／筆數，沒有 userId、accountId 或 reference 查詢條件，避免 browser 越權。
 * response 的 amount 和 bigint identifier 均為字串，以保留資產精度與避免 JavaScript safe-integer 截斷。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/assets/history")
public class AssetLedgerHistoryQueryController {
    private static final String SOURCE = "IMMUTABLE_LEDGER";
    private final AssetLedgerHistoryQueryService service;

    public AssetLedgerHistoryQueryController(AssetLedgerHistoryQueryService service) {
        this.service = service;
    }

    @GetMapping
    public AssetLedgerHistoryResponse list(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String beforePostedAt,
        @RequestParam(required = false) String beforeEntryId
    ) {
        AssetLedgerHistoryPage page = service.listFor(authenticatedUser, parseCursor(beforePostedAt, beforeEntryId), limit);
        return new AssetLedgerHistoryResponse(SOURCE, page.items().stream().map(AssetLedgerHistoryItemResponse::from).toList(),
            page.nextCursor() == null ? null : AssetLedgerHistoryCursorResponse.from(page.nextCursor()));
    }

    private static Optional<AssetLedgerHistoryCursor> parseCursor(String postedAt, String entryId) {
        if (postedAt == null && entryId == null) return Optional.empty();
        if (postedAt == null || entryId == null) throw new InvalidAssetHistoryQueryException();
        try {
            return Optional.of(new AssetLedgerHistoryCursor(Instant.parse(postedAt), Long.parseLong(entryId)));
        } catch (IllegalArgumentException exception) {
            throw new InvalidAssetHistoryQueryException();
        }
    }

    public record AssetLedgerHistoryResponse(String source, List<AssetLedgerHistoryItemResponse> items, AssetLedgerHistoryCursorResponse nextCursor) { }
    public record AssetLedgerHistoryCursorResponse(String postedAt, String entryId) {
        static AssetLedgerHistoryCursorResponse from(AssetLedgerHistoryCursor cursor) {
            return new AssetLedgerHistoryCursorResponse(cursor.postedAt().toString(), Long.toString(cursor.entryId()));
        }
    }
    public record AssetLedgerHistoryItemResponse(String entryId, String journalId, String accountType, String assetSymbol,
                                                 String direction, String amount, String referenceType, String referenceId,
                                                 Instant postedAt, Instant recordedAt) {
        static AssetLedgerHistoryItemResponse from(AssetLedgerHistoryItem item) {
            return new AssetLedgerHistoryItemResponse(Long.toString(item.entryId()), Long.toString(item.journalId()),
                item.accountType().name(), item.assetSymbol().value(), item.direction(), item.amount().value().toPlainString(),
                item.referenceType(), item.referenceId(), item.postedAt(), item.recordedAt());
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class InvalidAssetHistoryQueryException extends RuntimeException { }
}
