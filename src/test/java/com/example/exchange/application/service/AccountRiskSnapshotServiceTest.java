/*
 * 檔案用途：測試 account risk snapshot 持久化服務。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.AccountRiskSnapshotStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRiskSnapshotServiceTest {

    @Test
    @DisplayName("persist 會計算並保存單一帳戶風險快照")
    /**
     * 流程：建立帳戶與 open position -> seed oracle mark price -> persist snapshot -> 驗證 store 可查 latest。
     */
    void persistStoresSingleAccountRiskSnapshot() {
        Fixtures fixtures = fixtures();
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        Account account = new Account(91);
        account.deposit(new BigDecimal("1000"));
        fixtures.accountRepository.save(account);
        fixtures.positionRepository.save(Position.builder()
                .uid(91)
                .symbol(symbol)
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .build());
        fixtures.oracle.update("BTCUSDT", new BigDecimal("120"), new BigDecimal("119"), "test");

        AccountRiskSnapshot snapshot = fixtures.snapshotService.persist(91);

        assertThat(snapshot.uid()).isEqualTo(91);
        assertThat(snapshot.unrealizedPnl()).isEqualByComparingTo("20.000000000000000000");
        assertThat(fixtures.snapshotStore.findLatest(91)).isPresent()
                .get()
                .extracting(AccountRiskSnapshot::totalEquity)
                .isEqualTo(new BigDecimal("1020.000000000000000000"));
    }

    @Test
    @DisplayName("persistKnownAccounts 會掃 account index 與 open-position index")
    /**
     * 流程：建立一個只有帳戶的 uid 與一個只有 open position 的 uid -> persistKnownAccounts -> 驗證兩者都被保存。
     */
    void persistKnownAccountsScansAccountsAndOpenPositions() {
        Fixtures fixtures = fixtures();
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        Account account = new Account(92);
        account.deposit(new BigDecimal("100"));
        fixtures.accountRepository.save(account);
        fixtures.positionRepository.save(Position.builder()
                .uid(93)
                .symbol(symbol)
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .build());
        fixtures.oracle.update("BTCUSDT", new BigDecimal("100"), new BigDecimal("100"), "test");

        List<AccountRiskSnapshot> snapshots = fixtures.snapshotService.persistKnownAccounts();

        assertThat(snapshots).extracting(AccountRiskSnapshot::uid)
                .containsExactly(92L, 93L);
        assertThat(fixtures.snapshotStore.findByUid(92, 10)).hasSize(1);
        assertThat(fixtures.snapshotStore.findByUid(93, 10)).hasSize(1);
    }

    private static Fixtures fixtures() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemPositionRepository positionRepository = new MemPositionRepository();
        MemAccountRiskSnapshotStore snapshotStore = new MemAccountRiskSnapshotStore();
        SymbolConfigRepository symbolConfigRepository = symbol -> Optional.of(SymbolConfig.builder()
                .symbol(symbol)
                .maintenanceMarginRate(new BigDecimal("0.005"))
                .build());
        AccountRiskService accountRiskService = new AccountRiskService(
                accountRepository,
                positionRepository,
                symbolConfigRepository
        );
        MarkPriceOracleService oracle = new MarkPriceOracleService(new MarkPriceOracleProperties());
        accountRiskService.setMarkPriceOracleService(oracle);

        return new Fixtures(
                accountRepository,
                positionRepository,
                snapshotStore,
                oracle,
                new AccountRiskSnapshotService(
                        accountRiskService,
                        snapshotStore,
                        accountRepository,
                        positionRepository
                )
        );
    }

    private record Fixtures(
            MemAccountRepository accountRepository,
            MemPositionRepository positionRepository,
            MemAccountRiskSnapshotStore snapshotStore,
            MarkPriceOracleService oracle,
            AccountRiskSnapshotService snapshotService
    ) {
    }

    private static class MemAccountRiskSnapshotStore implements AccountRiskSnapshotStore {
        private final Map<Long, List<AccountRiskSnapshot>> snapshots = new LinkedHashMap<>();

        @Override
        public void save(AccountRiskSnapshot snapshot) {
            snapshots.computeIfAbsent(snapshot.uid(), ignored -> new ArrayList<>()).add(snapshot);
        }

        @Override
        public Optional<AccountRiskSnapshot> findLatest(long uid) {
            return findByUid(uid, 1).stream().findFirst();
        }

        @Override
        public List<AccountRiskSnapshot> findByUid(long uid, int limit) {
            return snapshots.getOrDefault(uid, List.of()).stream()
                    .sorted(Comparator.comparing(AccountRiskSnapshot::calculatedAt).reversed())
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        public List<Account> findAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        public void save(Account account) {
            accounts.put(account.uid(), account);
        }
    }

    private static class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream()
                    .filter(position -> position.getUid() == uid)
                    .toList();
        }

        @Override
        public List<Position> findOpenPositions() {
            return positions.values().stream()
                    .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                    .toList();
        }

        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }
}
