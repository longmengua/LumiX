package com.lumix.account.projection;

import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** owner identity 只能來自 authenticated session，不能由 inventory request 指定。 */
@Service
class AccountInventoryQueryService {
    private final AccountInventoryQueryRepository repository;
    AccountInventoryQueryService(AccountInventoryQueryRepository repository) { this.repository = repository; }
    List<AccountInventoryItem> find(AuthenticatedUser owner) { Objects.requireNonNull(owner, "owner"); return List.copyOf(repository.findByOwnerUserId(owner.userId())); }
}
