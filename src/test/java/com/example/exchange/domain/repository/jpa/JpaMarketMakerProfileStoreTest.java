/*
 * 檔案用途：測試做市商 profile JPA store 的寫入順序，避免既有 profile 更新撞唯一鍵。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class JpaMarketMakerProfileStoreTest {

    private final MarketMakerProfileRecordJpaRepository profileRepository =
            mock(MarketMakerProfileRecordJpaRepository.class);
    private final MarketMakerRiskLimitRecordJpaRepository riskLimitRepository =
            mock(MarketMakerRiskLimitRecordJpaRepository.class);
    private final JpaMarketMakerProfileStore store =
            new JpaMarketMakerProfileStore(profileRepository, riskLimitRepository);

    @Test
    @DisplayName("更新既有做市商時，先 flush 舊 risk limit 刪除再插入同 symbol 新資料")
    void saveFlushesRiskLimitDeletesBeforeReplacingSameSymbols() {
        MarketMakerProfile profile = new MarketMakerProfile(
                "mm-alpha",
                90001L,
                false,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("100000"),
                        new BigDecimal("100000"),
                        new BigDecimal("5000"),
                        new BigDecimal("0.01"),
                        false
                ))
        );

        // Flow: profile toggle/edit reuses the same marketMakerId+symbol, so delete must hit DB before insert.
        store.save(profile);

        InOrder writes = inOrder(profileRepository, riskLimitRepository);
        writes.verify(profileRepository).save(any());
        writes.verify(riskLimitRepository).deleteByMarketMakerId("mm-alpha");
        writes.verify(riskLimitRepository).flush();
        writes.verify(riskLimitRepository).saveAll(any());
    }
}
