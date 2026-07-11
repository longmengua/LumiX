package com.lumix.trading.core.spot.orderbook;

import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox order book 的設計契約。
 *
 * 這份契約只描述 in-memory book 與查詢邊界，不代表正式 order book / matching engine 已完成。
 */
public record SpotSandboxOrderBookDesign(
        List<SpotSandboxOrderStatus> supportedStatuses,
        List<String> bookRules,
        List<String> duplicateRules,
        List<String> noGoConditions
) {

    /**
     * 建立不可變的 order book 設計輸出。
     *
     * 這裡要先複製集合，避免後續測試或文件讀取時誤把設計資料當成可變 runtime 狀態。
     */
    public SpotSandboxOrderBookDesign {
        Objects.requireNonNull(supportedStatuses, "supportedStatuses must not be null");
        Objects.requireNonNull(bookRules, "bookRules must not be null");
        Objects.requireNonNull(duplicateRules, "duplicateRules must not be null");
        Objects.requireNonNull(noGoConditions, "noGoConditions must not be null");
        supportedStatuses = List.copyOf(supportedStatuses);
        bookRules = List.copyOf(bookRules);
        duplicateRules = List.copyOf(duplicateRules);
        noGoConditions = List.copyOf(noGoConditions);
    }
}
