package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.OrderBookSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRiskServiceTest {

    @Test
    void missingAccountReturnsZeroRiskSnapshot() {
        AccountRiskService service = service(new MemAccountRepository(), new MemPositionRepository(), new MarketDataService());

        AccountRiskSnapshot snapshot = service.snapshot(21);

        assertThat(snapshot.uid()).isEqualTo(21);
        assertThat(snapshot.crossBalance()).isEqualByComparingTo("0");
        assertThat(snapshot.totalEquity()).isEqualByComparingTo("0");
        assertThat(snapshot.maintenanceMargin()).isEqualByComparingTo("0");
        assertThat(snapshot.riskRatio()).isEqualByComparingTo("0");
        assertThat(snapshot.openPositionCount()).isZero();
    }

    @Test
    void snapshotUsesMarkPriceForEquityMaintenanceMarginAndRiskRatio() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemPositionRepository positionRepository = new MemPositionRepository();
        MarketDataService marketDataService = new MarketDataService();
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();

        Account account = new Account(22);
        account.deposit(new BigDecimal("1000.00"));
        account.reserveOrder(new BigDecimal("30.00"));
        account.reservePositionMargin(new BigDecimal("100.00"));
        accountRepository.save(account);
        positionRepository.save(Position.builder()
                .uid(22)
                .symbol(symbol)
                .qty(new BigDecimal("2.000"))
                .entryPrice(new BigDecimal("100.00"))
                .margin(new BigDecimal("100.00"))
                .build());
        marketDataService.onTrades(
                "BTCUSDT",
                List.of(new TradeExecuted(22, symbol, new BigDecimal("1.000"), new BigDecimal("110.00"), 1, Instant.now())),
                new OrderBookSnapshot(List.of(), List.of()),
                Optional.empty()
        );

        AccountRiskSnapshot snapshot = service(accountRepository, positionRepository, marketDataService).snapshot(22);

        assertThat(snapshot.crossBalance()).isEqualByComparingTo("1000.00");
        assertThat(snapshot.availableBalance()).isEqualByComparingTo("870.00");
        assertThat(snapshot.frozenFunds()).isEqualByComparingTo("130.00");
        assertThat(snapshot.unrealizedPnl()).isEqualByComparingTo("20.000000000000000000");
        assertThat(snapshot.totalEquity()).isEqualByComparingTo("1020.000000000000000000");
        assertThat(snapshot.maintenanceMargin()).isEqualByComparingTo("1.100000000000000000");
        assertThat(snapshot.riskRatio()).isEqualByComparingTo("0.001078431372549020");
        assertThat(snapshot.openPositionCount()).isOne();
    }

    private static AccountRiskService service(
            AccountRepository accountRepository,
            PositionRepository positionRepository,
            MarketDataService marketDataService
    ) {
        SymbolConfigRepository symbolConfigRepository = symbol -> Optional.of(SymbolConfig.builder()
                .symbol(symbol)
                .maintenanceMarginRate(new BigDecimal("0.005"))
                .build());
        return new AccountRiskService(accountRepository, positionRepository, symbolConfigRepository, marketDataService);
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
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
