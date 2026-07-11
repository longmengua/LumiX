package com.lumix.trading.core.spot.orderbook;

import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderCommand;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderDecision;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeBoundary;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeResult;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox in-memory order book boundary。
 *
 * 這個 boundary 只負責把已接受的 intake result 轉成 sandbox record，並放入 in-memory book，不代表正式 order placement runtime。
 */
public final class SpotSandboxOrderBookBoundary {

    private final SpotSandboxOrderIntakeBoundary intakeBoundary;
    private final InMemorySpotSandboxOrderBook orderBook;
    private final SpotSandboxOrderBookPolicy policy = new SpotSandboxOrderBookPolicy();

    /**
     * 建立預設的 spot sandbox order book boundary。
     *
     * 這個 boundary 只在 sandbox 裡串接 intake 與 in-memory book，不會碰任何 DB 或 production runtime。
     */
    public SpotSandboxOrderBookBoundary() {
        this(new SpotSandboxOrderIntakeBoundary(), new InMemorySpotSandboxOrderBook());
    }

    /**
     * 建立可注入依賴的 spot sandbox order book boundary。
     *
     * 這個 constructor 只為測試與 boundary 組裝使用，不代表有正式 application service。
     */
    public SpotSandboxOrderBookBoundary(SpotSandboxOrderIntakeBoundary intakeBoundary, InMemorySpotSandboxOrderBook orderBook) {
        this.intakeBoundary = Objects.requireNonNull(intakeBoundary, "intakeBoundary must not be null");
        this.orderBook = Objects.requireNonNull(orderBook, "orderBook must not be null");
    }

    /**
     * 受理 sandbox order command，並將 accepted command 放入 in-memory book。
     *
     * 這裡先做 P16-T02 intake validation，再做 P16-T03 order book materialization。
     */
    public SpotSandboxOrderBookResult accept(SpotSandboxOrderCommand command, Instant acceptedAt) {
        SpotSandboxOrderIntakeResult intakeResult = intakeBoundary.evaluate(command);
        return accept(intakeResult, acceptedAt);
    }

    /**
     * 受理已完成 intake validation 的結果，並將 accepted result 放入 in-memory book。
     *
     * rejected intake result 不能進 book，duplicate 只會回傳既有 record。
     */
    public SpotSandboxOrderBookResult accept(SpotSandboxOrderIntakeResult intakeResult, Instant acceptedAt) {
        Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
        if (intakeResult == null) {
            return SpotSandboxOrderBookResult.rejected(
                    SpotSandboxOrderBookRejectionReason.INTAKE_REJECTED,
                    "intake result 不可為 null"
            );
        }

        if (intakeResult.decision() != SpotSandboxOrderDecision.ACCEPTED) {
            return SpotSandboxOrderBookResult.rejected(
                    SpotSandboxOrderBookRejectionReason.INTAKE_REJECTED,
                    intakeResult.rejection() == null ? "intake result 未通過驗證" : intakeResult.rejection().message()
            );
        }

        return orderBook.accept(intakeResult, acceptedAt);
    }

    /**
     * 查詢指定 market 的 open orders。
     *
     * 這個查詢只回傳 in-memory book 內的 open records，不代表任何 production order book。
     */
    public List<SpotSandboxOrderRecord> openOrders(String marketSymbol) {
        return orderBook.openOrders(marketSymbol);
    }

    /**
     * 回傳 sandbox order book policy。
     *
     * 這個方法只提供設計契約閱讀，不代表 boundary 已經接上任何 persistence runtime。
     */
    public SpotSandboxOrderBookPolicy policy() {
        return policy;
    }
}
