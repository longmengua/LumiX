package com.lumix.account.projection;

import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 使用者資產 projection 的 application query service。
 *
 * <p>owner 永遠來自已驗證 session 的 principal，而非 URL 或 query parameter。此服務不建立帳戶、
 * 不重建 projection，也不會更新 ledger、available 或 locked amount。</p>
 */
@Service
class BalanceProjectionQueryService {

    private final BalanceProjectionQueryRepository repository;

    BalanceProjectionQueryService(BalanceProjectionQueryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /** 讀取登入者自己的現有 projection rows；空集合代表沒有可呈現的真實 projection，不以假零餘額替代。 */
    List<AccountBalanceProjection> listFor(AuthenticatedUser authenticatedUser) {
        Objects.requireNonNull(authenticatedUser, "authenticatedUser must not be null");
        return List.copyOf(repository.findByOwnerUserId(authenticatedUser.userId()));
    }
}
