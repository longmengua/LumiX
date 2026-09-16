package com.lumix.account.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AssetLedgerHistoryQueryServiceTest {
    /** 多取一筆時必須只回傳 requested limit，並將最後一筆變成穩定的下一頁 cursor。 */
    @Test
    void returnsKeysetCursorWithoutChangingOwnerScope() {
        AssetLedgerHistoryQueryRepository repository = (owner, before, limit) -> List.of(item(3), item(2), item(1));
        AssetLedgerHistoryQueryService service = new AssetLedgerHistoryQueryService(repository);

        AssetLedgerHistoryPage page = service.listFor(new AuthenticatedUser("owner", "owner@example.com", "Owner"), Optional.empty(), 2);

        assertEquals(List.of(3L, 2L), page.items().stream().map(AssetLedgerHistoryItem::entryId).toList());
        assertEquals(2L, page.nextCursor().entryId());
        assertEquals(Instant.parse("2026-09-15T00:00:02Z"), page.nextCursor().postedAt());
    }

    /** 無界或負值 limit 會被拒絕，避免歷史 endpoint 被用作大量全表讀取。 */
    @Test
    void rejectsUnboundedHistoryRequest() {
        AssetLedgerHistoryQueryService service = new AssetLedgerHistoryQueryService((owner, before, limit) -> List.of());
        AuthenticatedUser user = new AuthenticatedUser("owner", "owner@example.com", "Owner");

        assertThrows(IllegalArgumentException.class, () -> service.listFor(user, Optional.empty(), 0));
        assertThrows(IllegalArgumentException.class, () -> service.listFor(user, Optional.empty(), 101));
    }

    private static AssetLedgerHistoryItem item(long id) {
        return new AssetLedgerHistoryItem(id, id, AccountType.SPOT, new AssetSymbol("USDT"), "CREDIT",
            new MoneyAmount(new BigDecimal("1.000000000000000001")), "TRADE", "ref-" + id,
            Instant.parse("2026-09-15T00:00:0" + id + "Z"), Instant.parse("2026-09-15T00:01:00Z"));
    }
}
