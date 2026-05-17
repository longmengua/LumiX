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

    /** 金額與比例統一用 18 位小數，避免不同來源 scale 導致 API 回傳不穩定。 */
    private static final int SCALE = 18;

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final MarketDataService marketDataService;

    /**
     * 建立指定使用者的即時計算快照。
     *
     * <p>此方法不寫入狀態，只讀取 Account、Position、SymbolConfig 與 MarketData。
     * 沒有帳戶時回傳零值快照，讓前端或營運工具可以安全查詢新使用者。</p>
     */
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

    /** 多空都用 (mark - entry) * signedQty；空倉 qty 為負數，因此公式自然反向。 */
    private BigDecimal unrealizedPnl(Position position) {
        BigDecimal qty = safe(position.getQty());
        if (qty.signum() == 0) return BigDecimal.ZERO;
        BigDecimal markPrice = markPrice(position);
        BigDecimal entryPrice = safe(position.getEntryPrice());
        return markPrice.subtract(entryPrice).multiply(qty);
    }

    /** 維持保證金以 mark notional * symbol maintenance margin rate 計算。 */
    private BigDecimal maintenanceMargin(Position position) {
        BigDecimal qty = safe(position.getQty()).abs();
        if (qty.signum() == 0) return BigDecimal.ZERO;
        BigDecimal notional = markPrice(position).multiply(qty);
        BigDecimal maintenanceRate = symbolConfig(position).maintenanceMarginRateOrDefault();
        return notional.multiply(maintenanceRate);
    }

    /** 找不到 symbol config 時使用預設 SymbolConfig，避免風險查詢因設定缺口中斷。 */
    private SymbolConfig symbolConfig(Position position) {
        String symbol = position.getSymbol() == null ? "" : position.getSymbol().code();
        return symbolConfigRepository.findBySymbol(symbol)
                .orElseGet(() -> SymbolConfig.builder().symbol(symbol).build());
    }

    /** MVP 使用 ticker last price 當 mark price；無行情時退回 entry price。 */
    private BigDecimal markPrice(Position position) {
        String symbol = position.getSymbol() == null ? "" : position.getSymbol().code();
        return marketDataService.ticker(symbol)
                .map(MarketTicker::lastPrice)
                .filter(price -> price != null && price.signum() > 0)
                .orElse(safe(position.getEntryPrice()));
    }

    /** equity 非正且仍有維持保證金時視為 100% 風險，避免除零。 */
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
