package com.lumix.marketdata.contract;

import java.util.List;

/**
 * P21-T04 唯讀 order-book contract 的固定資源邊界。
 *
 * <p>此上限刻意不是 runtime 設定：尚未有 provider 或 transport runtime，讓每次 replay 與所有呼叫端
 * 都對相同輸入得到相同的 fail-closed 結果，避免單一畸形 payload 佔用無界記憶體或 CPU。</p>
 */
public final class OrderBookLevelLimits {

    /** 每一側最多 1,024 個價位；snapshot、delta 與已發布 projection 均使用相同固定邊界。 */
    public static final int MAX_LEVELS_PER_SIDE = 1_024;

    private OrderBookLevelLimits() {
    }

    /**
     * 在輸入 payload 建構時拒絕超過每側上限的資料，避免 reducer 先複製或排序無界清單。
     */
    public static List<BookLevel> requirePayloadSideWithinLimit(List<BookLevel> levels, String fieldName) {
        if (levels.size() > MAX_LEVELS_PER_SIDE) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.BOOK_LEVEL_LIMIT_EXCEEDED,
                    fieldName + " exceeds fixed order-book level limit"
            );
        }
        return levels;
    }

    /**
     * projection 是公開 immutable model，仍須自行守住上限，避免呼叫端繞過 reducer 建立過大的唯讀快照。
     */
    public static void requireProjectionSideWithinLimit(List<BookLevel> levels, String fieldName) {
        if (levels.size() > MAX_LEVELS_PER_SIDE) {
            throw new IllegalArgumentException(fieldName + " exceeds fixed order-book level limit");
        }
    }
}
