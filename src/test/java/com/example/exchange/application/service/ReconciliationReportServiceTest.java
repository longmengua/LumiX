/*
 * 檔案用途：測試 persisted reconciliation report 產生與查詢。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.ReconciliationReportResult;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.ReconciliationReportStore;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.ReconciliationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationReportServiceTest {

    @Test
    @DisplayName("runAndPersist 會保存 reconciliation report summary 與 issue 明細")
    /**
     * 流程：建立一個 position margin 不一致帳戶 -> 產生 report -> 驗證 summary、issue 與 latest 查詢。
     */
    void runAndPersistStoresReportAndIssues() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemPositionRepository positionRepository = new MemPositionRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        MemReconciliationReportStore reportStore = new MemReconciliationReportStore();
        Account account = new Account(81);
        account.deposit(new BigDecimal("100"));
        account.reservePositionMargin(new BigDecimal("10"));
        accountRepository.save(account);

        ReconciliationReportService service = new ReconciliationReportService(
                new ReconciliationService(accountRepository, positionRepository, ledgerRepository),
                reportStore,
                properties()
        );

        ReconciliationReportResult result = service.runAndPersist("manual");

        assertThat(result.triggeredBy()).isEqualTo("MANUAL");
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.scannedAccounts()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.warnCount()).isZero();
        assertThat(result.issues()).extracting(issue -> issue.code())
                .containsExactly("POSITION_MARGIN_MISMATCH");
        assertThat(service.findById(result.id())).isPresent();
        assertThat(service.latest(10)).extracting(ReconciliationReportResult::id)
                .containsExactly(result.id());
    }

    @Test
    @DisplayName("event-store coverage 缺口會產生 WARN issue")
    /**
     * 流程：帳戶有 open position 且 EventStore lastSeq=0 -> validateUid 回報 event-store coverage warning。
     */
    void eventStoreCoverageMissingCreatesWarningIssue() {
        MemAccountRepository accountRepository = new MemAccountRepository();
        MemPositionRepository positionRepository = new MemPositionRepository();
        MemWalletLedgerRepository ledgerRepository = new MemWalletLedgerRepository();
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        Account account = new Account(82);
        account.deposit(new BigDecimal("100"));
        account.reservePositionMargin(new BigDecimal("5"));
        accountRepository.save(account);
        positionRepository.save(Position.builder()
                .uid(82)
                .symbol(symbol)
                .qty(new BigDecimal("1"))
                .entryPrice(new BigDecimal("100"))
                .margin(new BigDecimal("5"))
                .build());

        ReconciliationService service = new ReconciliationService(accountRepository, positionRepository, ledgerRepository);
        service.setEventStore(new EmptyEventStore());

        assertThat(service.validateUid(82)).extracting(issue -> issue.code())
                .containsExactly("EVENT_STORE_COVERAGE_MISSING");
    }

    private static ReconciliationProperties properties() {
        ReconciliationProperties properties = new ReconciliationProperties();
        properties.setAlertOnError(false);
        properties.setAlertRoute("test.reconciliation");
        return properties;
    }

    private static class MemReconciliationReportStore implements ReconciliationReportStore {
        private final Map<String, ReconciliationReport> reports = new LinkedHashMap<>();
        private final Map<String, List<ReconciliationReportIssue>> issues = new LinkedHashMap<>();

        @Override
        public void save(ReconciliationReport report, List<ReconciliationReportIssue> issues) {
            reports.put(report.getId(), report);
            this.issues.put(report.getId(), issues);
        }

        @Override
        public Optional<ReconciliationReport> findById(String reportId) {
            return Optional.ofNullable(reports.get(reportId));
        }

        @Override
        public List<ReconciliationReportIssue> findIssues(String reportId) {
            return issues.getOrDefault(reportId, List.of());
        }

        @Override
        public List<ReconciliationReport> latest(int limit) {
            return reports.values().stream()
                    .limit(limit)
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

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        @Override
        public void append(WalletLedgerEntry entry) {
        }

        @Override
        public List<WalletLedgerEntry> findByUid(long uid) {
            return List.of();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return List.of();
        }
    }

    private static class EmptyEventStore implements com.example.exchange.domain.repository.EventStore {
        @Override
        public long append(com.example.exchange.domain.event.TradeExecuted event) {
            return 0;
        }

        @Override
        public long lastSeq(long uid) {
            return 0;
        }
    }
}
