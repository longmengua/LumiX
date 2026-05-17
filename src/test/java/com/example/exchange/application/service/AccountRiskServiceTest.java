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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 AccountRiskService 的即時帳戶風險快照。
 *
 * <p>重點是確認沒有帳戶時安全回零，以及有持倉/行情時會正確計算
 * frozen funds、未實現盈虧、equity、維持保證金與風險率。</p>
 */
class AccountRiskServiceTest {

    @Test
    @DisplayName("帳戶不存在時回傳零值風險快照")
    /**
     * 流程：建立空 repository -> 查詢不存在 uid 的風險快照 -> 驗證所有資產與風險欄位安全回零。
     */
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
    @DisplayName("使用 mark price 計算 equity、maintenance margin 與 risk ratio")
    /**
     * 流程：準備帳戶凍結資金、持倉與最新成交價 -> 呼叫 snapshot -> 驗證 available、PNL、equity 與 risk ratio。
     */
    void snapshotUsesMarkPriceForEquityMaintenanceMarginAndRiskRatio() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemPositionRepository positionRepository = new MemPositionRepository();
        MarketDataService marketDataService = new MarketDataService();
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();

        // 1000 餘額中，30 被訂單凍結，100 被持倉保證金凍結。
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
        // 製造 last price = 110，讓 risk snapshot 不必 fallback 到 entry price。
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

    /**
     * 建立 AccountRiskService 測試鏈路，把 account、position、market data 與固定維持保證金率接起來。
     */
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
        /**
         * 模擬依 uid 查帳戶；不存在時回 empty，讓 missing-account 分支可被驗證。
         */
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        /**
         * 保存帳戶目前狀態，供 snapshot 後續讀取 balance、hold 與 margin。
         */
        public void save(Account account) {
            accounts.put(account.uid(), account);
        }
    }

    private static class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        /**
         * 依 uid + symbol 查單一持倉，支援 service 讀取指定 market 的風險資料。
         */
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        /**
         * 保存持倉狀態，讓測試可先建立 open position 再計算風險快照。
         */
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        /**
         * 回傳某 uid 的全部持倉，AccountRiskService 會用它彙總 open position 風險。
         */
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream()
                    .filter(position -> position.getUid() == uid)
                    .toList();
        }

        @Override
        /**
         * 回傳所有未平倉部位；此檔主要測 uid snapshot，仍保留介面行為避免 stub 不完整。
         */
        public List<Position> findOpenPositions() {
            return positions.values().stream()
                    .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                    .toList();
        }

        /**
         * 建立 map key，避免同一 uid 在不同 symbol 的持倉互相覆蓋。
         */
        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }
}
