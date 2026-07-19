package com.lumix.trading.core.futures.sandbox.funding.mock;

import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingPreview;
import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingPreviewCalculator;
import java.util.List;
import java.util.Objects;

/**
 * Phase 19-T02 的 pure funding mock batch simulator。
 *
 * 它只重用 P18-T05 的單一 position preview 計算，並固定 scenario 的 market/rate/time 一致性；
 * 不建立 funding schedule、不讀取外部行情，也不執行 payment、balance、ledger 或 settlement。
 */
public final class FuturesSandboxFundingMockBatchSimulator {

    private final FuturesSandboxFundingPreviewCalculator previewCalculator = new FuturesSandboxFundingPreviewCalculator();

    /**
     * 將受限 scenario 的每個 position input 轉為獨立 funding preview。
     */
    public FuturesSandboxFundingMockBatchResult simulate(FuturesSandboxFundingMockBatchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        List<FuturesSandboxFundingPreview> previews = request.previews().stream().map(previewCalculator::preview).toList();
        return new FuturesSandboxFundingMockBatchResult(previews);
    }
}
