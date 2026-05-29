/*
 * 檔案用途：領域 DTO，承載單筆 ADL forced execution 的實際結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * ADL forced execution step result。
 *
 * @param rank                  ADL queue rank
 * @param uid                   被強制減倉的使用者
 * @param symbol                normalized symbol code
 * @param executionPrice        本次強制減倉價格
 * @param closedQty             帶正負號的平倉數量；多單為負，空單為正
 * @param realizedPnl           本次減倉實現損益
 * @param marginReleased        依減倉比例釋放的 position margin
 * @param socializedLossCharged 從獲利帳戶扣回、用於承接 ADL 缺口的金額
 */
public record AdlExecutionStepResult(
        int rank,
        long uid,
        String symbol,
        BigDecimal executionPrice,
        BigDecimal closedQty,
        BigDecimal realizedPnl,
        BigDecimal marginReleased,
        BigDecimal socializedLossCharged
) {
}
