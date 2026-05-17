/*
 * 檔案用途：應用服務，提供帳戶風險與資產快照計算。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountRiskService {

    private static final int SCALE = 18;

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final MarketDataService marketDataService;

    public AccountRiskSnapshot snapshot(long uid) {
        Account account = accountRepository.findByUid(uid).orElseGet(() -> new Account(uid));
        List<Position> positions = positionRepository.findAllByUid(uid).stream()
                .filter(AccountRiskService::isOpen)
                .toList();

        BigDecimal unrealizedPnl = positions.stream()
                .map(this::unrealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maintenanceMargin = positions.stream()
                .map(this::maintenanceMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity = safe(account.crossBalance()).add(unrealizedPnl)
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new AccountRiskSnapshot(
                uid,
                safe(account.crossBalance()),
                safe(account.crossAvailable()),
                safe(account.crossOrderHold()),
                safe(account.crossPositionMargin()),
                safe(account.crossHold()),
                unrealizedPnl.setScale(SCALE, RoundingMode.HALF_UP),
                totalEquity,
                maintenanceMargin.setScale(SCALE, RoundingMode.HALF_UP),
                riskRatio(maintenanceMargin, totalEquity),
                positions.size(),
                Instant.now()
        );
    }

    private BigDecimal unrealizedPnl(Position position) {
        BigDecimal qty = safe(position.getQty());
        if (qty.signum() == 0) return BigDecimal.ZERO;
        BigDecimal markPrice = markPrice(position);
        BigDecimal entryPrice = safe(position.getEntryPrice());
        return markPrice.subtract(entryPrice).multiply(qty);
    }

    private BigDecimal maintenanceMargin(Position position) {
        BigDecimal qty = safe(position.getQty()).abs();
        if (qty.signum() == 0) return BigDecimal.ZERO;
        BigDecimal notional = markPrice(position).multiply(qty);
        BigDecimal maintenanceRate = symbolConfig(position).maintenanceMarginRateOrDefault();
        return notional.multiply(maintenanceRate);
    }

    private SymbolConfig symbolConfig(Position position) {
        String symbol = position.getSymbol() == null ? "" : position.getSymbol().code();
        return symbolConfigRepository.findBySymbol(symbol)
                .orElseGet(() -> SymbolConfig.builder().symbol(symbol).build());
    }

    private BigDecimal markPrice(Position position) {
        String symbol = position.getSymbol() == null ? "" : position.getSymbol().code();
        return marketDataService.ticker(symbol)
                .map(MarketTicker::lastPrice)
                .filter(price -> price != null && price.signum() > 0)
                .orElse(safe(position.getEntryPrice()));
    }

    private static BigDecimal riskRatio(BigDecimal maintenanceMargin, BigDecimal totalEquity) {
        BigDecimal maintenance = safe(maintenanceMargin);
        BigDecimal equity = safe(totalEquity);
        if (maintenance.signum() <= 0) return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        if (equity.signum() <= 0) return BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);
        return maintenance.divide(equity, SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isOpen(Position position) {
        return position != null && safe(position.getQty()).signum() != 0;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
