/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.PositionLiquidated;
import com.example.exchange.domain.model.dto.LiquidationResult;
import com.example.exchange.domain.model.dto.PositionChange;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiquidationService {

    private static final int MONEY_SCALE = 18;
    private static final BigDecimal PARTIAL_CLOSE_RATE = new BigDecimal("0.5");

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final WalletLedgerService walletLedgerService;
    private final InsuranceFundService insuranceFundService;
    private final DomainEventPublisher<PositionLiquidated> publisher;
    private MarkPriceOracleService markPriceOracleService;

    @Autowired(required = false)
    public void setMarkPriceOracleService(MarkPriceOracleService markPriceOracleService) {
        this.markPriceOracleService = markPriceOracleService;
    }

    public LiquidationResult liquidate(long uid, String symbol) {
        return liquidate(uid, symbol, requireOracleMarkPrice(symbol));
    }

    public LiquidationResult liquidate(long uid, String symbol, BigDecimal markPrice) {
        SymbolConfig config = symbolConfigRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + symbol));
        Position position = positionRepository.find(uid, config.toSymbol()).orElse(null);
        Instant now = Instant.now();
        String liquidationId = "liq-" + UUID.randomUUID();

        if (position == null || position.getQty() == null || position.getQty().signum() == 0) {
            return notLiquidated(uid, config, markPrice, liquidationId, now, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Account account = accountRepository.findByUid(uid).orElseGet(() -> new Account(uid));
        BigDecimal notional = position.getQty().abs().multiply(markPrice);
        BigDecimal maintenanceMarginRate = config.maintenanceMarginRateForNotional(notional);
        BigDecimal maintenanceMargin = notional.multiply(maintenanceMarginRate);
        BigDecimal equity = account.crossBalance().add(unrealizedPnl(position, markPrice));
        if (equity.compareTo(maintenanceMargin) >= 0) {
            return notLiquidated(uid, config, markPrice, liquidationId, now, maintenanceMargin, equity);
        }

        BigDecimal oldQty = position.getQty();
        BigDecimal oldMargin = safe(position.getMargin());
        BigDecimal closedQty = closeQty(oldQty, markPrice, maintenanceMarginRate, equity);
        BigDecimal closeRatio = closedQty.abs().divide(oldQty.abs(), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal marginToRelease = oldMargin.multiply(closeRatio);
        PositionChange change = position.applyTradeWithPnl(closedQty, markPrice);
        BigDecimal realizedPnl = change.realizedPnl();

        BigDecimal releasableMargin = marginToRelease.min(account.crossPositionMargin());
        if (releasableMargin.signum() > 0) {
            walletLedgerService.releasePositionMargin(uid, config.getQuoteAsset(), releasableMargin, liquidationId);
        }

        BigDecimal insuranceCovered = BigDecimal.ZERO;
        BigDecimal adlCovered = BigDecimal.ZERO;
        if (realizedPnl.signum() < 0) {
            BigDecimal loss = realizedPnl.abs();
            Account beforeLoss = walletLedgerService.getOrCreate(uid);
            BigDecimal shortfall = loss.subtract(beforeLoss.crossBalance());
            if (shortfall.signum() > 0) {
                insuranceCovered = insuranceFundService.cover(config.getQuoteAsset(), shortfall);
                walletLedgerService.applyInsurancePayout(uid, config.getQuoteAsset(), insuranceCovered, liquidationId);

                adlCovered = shortfall.subtract(insuranceCovered);
                if (adlCovered.signum() > 0) {
                    insuranceFundService.enqueueAdl(liquidationId, uid, config.getSymbol(), adlCovered);
                    walletLedgerService.applyAdlCompensation(uid, config.getQuoteAsset(), adlCovered, liquidationId);
                }
            }
        }

        walletLedgerService.applyRealizedPnl(uid, config.getQuoteAsset(), realizedPnl, liquidationId);
        BigDecimal remainingMargin = oldMargin.subtract(marginToRelease);
        position.setMargin(position.getQty().signum() == 0 || remainingMargin.signum() < 0
                ? BigDecimal.ZERO
                : remainingMargin);
        position.addInsuranceFundCovered(insuranceCovered);
        position.addAdlCovered(adlCovered);
        positionRepository.save(position);

        publisher.publish(new PositionLiquidated(
                uid,
                config.toSymbol(),
                markPrice,
                markPrice,
                closedQty,
                realizedPnl,
                insuranceCovered,
                adlCovered,
                now
        ));

        return new LiquidationResult(
                uid,
                config.getSymbol(),
                true,
                markPrice,
                maintenanceMargin,
                equity,
                closedQty,
                realizedPnl,
                insuranceCovered,
                adlCovered,
                liquidationId,
                now
        );
    }

    private static LiquidationResult notLiquidated(
            long uid,
            SymbolConfig config,
            BigDecimal markPrice,
            String liquidationId,
            Instant now,
            BigDecimal maintenanceMargin,
            BigDecimal equity
    ) {
        return new LiquidationResult(
                uid,
                config.getSymbol(),
                false,
                markPrice,
                maintenanceMargin,
                equity,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                liquidationId,
                now
        );
    }

    private static BigDecimal unrealizedPnl(Position position, BigDecimal markPrice) {
        if (position.getQty().signum() > 0) {
            return markPrice.subtract(position.getEntryPrice()).multiply(position.getQty().abs());
        }
        return position.getEntryPrice().subtract(markPrice).multiply(position.getQty().abs());
    }

    private static BigDecimal closeQty(
            BigDecimal currentQty,
            BigDecimal markPrice,
            BigDecimal maintenanceMarginRate,
            BigDecimal equity
    ) {
        BigDecimal partialCloseQty = currentQty.negate().multiply(PARTIAL_CLOSE_RATE);
        BigDecimal remainingQty = currentQty.add(partialCloseQty);
        BigDecimal remainingMaintenance = remainingQty.abs()
                .multiply(markPrice)
                .multiply(maintenanceMarginRate);
        if (remainingQty.signum() != 0 && equity.compareTo(remainingMaintenance) >= 0) {
            return partialCloseQty;
        }
        return currentQty.negate();
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal requireOracleMarkPrice(String symbol) {
        if (markPriceOracleService == null) {
            throw new IllegalStateException("mark price oracle is not configured");
        }
        return markPriceOracleService.requireMarkPrice(symbol);
    }
}
