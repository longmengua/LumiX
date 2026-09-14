package com.lumix.account.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BalanceProjectionQueryServiceTest {

    /** owner 必須從 authenticated principal 取得，不能由 client 提供另一個 userId。 */
    @Test
    void readsOnlyTheAuthenticatedOwnersExistingProjectionRows() {
        BalanceProjectionQueryRepository repository = mock(BalanceProjectionQueryRepository.class);
        BalanceProjectionQueryService service = new BalanceProjectionQueryService(repository);
        AuthenticatedUser actor = new AuthenticatedUser("user-owner", "owner@example.com", "Owner");
        AccountBalanceProjection projection = projection("acct-owner", "10.250000000000000000", "10.000000000000000000", "0.250000000000000000");
        when(repository.findByOwnerUserId("user-owner")).thenReturn(List.of(projection));

        assertEquals(List.of(projection), service.listFor(actor));
        verify(repository).findByOwnerUserId("user-owner");
    }

    /** 沒有 materialized row 時回傳空集合，不可在 service 補造零餘額或 mock asset。 */
    @Test
    void keepsAnEmptyProjectionSetEmpty() {
        BalanceProjectionQueryRepository repository = mock(BalanceProjectionQueryRepository.class);
        BalanceProjectionQueryService service = new BalanceProjectionQueryService(repository);
        AuthenticatedUser actor = new AuthenticatedUser("user-empty", "empty@example.com", "Empty");
        when(repository.findByOwnerUserId("user-empty")).thenReturn(List.of());

        assertEquals(List.of(), service.listFor(actor));
        verify(repository).findByOwnerUserId("user-empty");
    }

    private static AccountBalanceProjection projection(String accountId, String total, String available, String locked) {
        return new AccountBalanceProjection(
            new AccountId(accountId), AccountType.SPOT, AccountStatus.ACTIVE, new AssetSymbol("USDT"), "Tether USD", "ACTIVE", 6,
            new MoneyAmount(new BigDecimal(total)), new MoneyAmount(new BigDecimal(available)), new MoneyAmount(new BigDecimal(locked)),
            8L, Instant.parse("2026-09-15T00:00:00Z"), Instant.parse("2026-09-15T00:00:05Z")
        );
    }
}
