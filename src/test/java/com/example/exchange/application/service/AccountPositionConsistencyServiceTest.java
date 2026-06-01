/*
 * 檔案用途：測試 restore 後 account / position consistency report。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AccountPositionConsistencyReport;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountPositionConsistencyServiceTest {

    @Test
    @DisplayName("validateAfterRestore 會回報 open position 沒有 restored account")
    void validateAfterRestoreReportsPositionWithoutAccount() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemPositionRepository positionRepository = new MemPositionRepository();
        positionRepository.positions.add(position(7, "1"));
        AccountPositionConsistencyService service =
                new AccountPositionConsistencyService(accountRepository, positionRepository);

        // 場景：position restore 成功但 account restore 缺失，DR smoke 必須阻止切回流量。
        AccountPositionConsistencyReport report = service.validateAfterRestore();

        assertThat(report.valid()).isFalse();
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().issueType()).isEqualTo("POSITION_WITHOUT_ACCOUNT");
    }

    @Test
    @DisplayName("validateAfterRestore 會回報 account position margin 低於 open position margin")
    void validateAfterRestoreReportsInsufficientAccountPositionMargin() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        Account account = new Account(8);
        account.restoreCross(new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("1"));
        accountRepository.accounts.add(account);
        MemPositionRepository positionRepository = new MemPositionRepository();
        positionRepository.positions.add(position(8, "2"));
        AccountPositionConsistencyService service =
                new AccountPositionConsistencyService(accountRepository, positionRepository);

        AccountPositionConsistencyReport report = service.validateAfterRestore();

        assertThat(report.valid()).isFalse();
        assertThat(report.issues().getFirst().issueType()).isEqualTo("ACCOUNT_MARGIN_BELOW_OPEN_POSITION_MARGIN");
    }

    @Test
    @DisplayName("validateAfterRestore 對 account margin 足夠的 restored state 回報 valid")
    void validateAfterRestorePassesWhenAccountAndPositionsAlign() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        Account account = new Account(9);
        account.restoreCross(new BigDecimal("100"), new BigDecimal("98"), BigDecimal.ZERO, new BigDecimal("2"));
        accountRepository.accounts.add(account);
        MemPositionRepository positionRepository = new MemPositionRepository();
        positionRepository.positions.add(position(9, "2"));
        AccountPositionConsistencyService service =
                new AccountPositionConsistencyService(accountRepository, positionRepository);

        AccountPositionConsistencyReport report = service.validateAfterRestore();

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
    }

    private static Position position(long uid, String margin) {
        return Position.builder()
                .uid(uid)
                .symbol(Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build())
                .mode(MarginMode.CROSS)
                .leverage(new BigDecimal("20"))
                .qty(BigDecimal.ONE)
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal(margin))
                .build();
    }

    private static class MemAccountRepository implements AccountRepository {
        private final List<Account> accounts = new ArrayList<>();

        @Override
        public Optional<Account> findByUid(long uid) {
            return accounts.stream().filter(account -> account.uid() == uid).findFirst();
        }

        @Override
        public List<Account> findAll() {
            return List.copyOf(accounts);
        }

        @Override
        public void save(Account account) {
            accounts.removeIf(existing -> existing.uid() == account.uid());
            accounts.add(account);
        }
    }

    private static class MemPositionRepository implements PositionRepository {
        private final List<Position> positions = new ArrayList<>();

        @Override
        public Optional<Position> find(long uid, Symbol symbol) {
            return positions.stream()
                    .filter(position -> position.getUid() == uid)
                    .filter(position -> position.getSymbol().code().equals(symbol.code()))
                    .findFirst();
        }

        @Override
        public void save(Position position) {
            positions.add(position);
        }

        @Override
        public List<Position> findAllByUid(long uid) {
            return positions.stream().filter(position -> position.getUid() == uid).toList();
        }

        @Override
        public List<Position> findOpenPositions() {
            return List.copyOf(positions);
        }
    }
}
