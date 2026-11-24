package com.lumix.trading.liquidity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.lumix.trading.runtime.TradingInstrumentRules;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 站內互撮後剩餘量的外部送單邊界。來源行情不實作此介面；只有具交易權限、可查單並可對帳的 adapter 才能接入。
 * 客戶成交量只可依站內或外部確定成交回報入帳；逾時或回報不明時查詢原命令，不得自行宣告成交。
 */
public interface ExternalOrderRouter {
    /** provider 對外識別碼，只能使用穩定且不含秘密的名稱。 */
    String source();

    /** 以穩定的 clientOrderId 送單；結果只描述外部狀態，不代表 LumiX 帳本已完成結算。 */
    Outcome submit(Intent intent);

    /** 依原 clientOrderId 查詢外部訂單，供逾時及程序重啟後恢復。 */
    Outcome lookup(String source, String clientOrderId);

    record Intent(String source, String clientOrderId, TradingInstrumentRules.Kind kind,
                  String marketSymbol, Side side, BigDecimal quantity, BigDecimal referencePrice, BigDecimal limitPrice,
                  TimeInForce timeInForce) { }

    enum Side { BUY, SELL }

    /** 外部剩餘量的 IOC／FOK 選擇由客戶命令與已核定政策決定。 */
    enum TimeInForce { IOC, FOK }

    enum Status {
        PENDING(1000, "Pending"), PARTIALLY_FILLED(1100, "Partially filled"),
        FILLED(1200, "Filled"), REJECTED(1400, "Rejected"),
        CANCELED(1500, "Canceled"), UNKNOWN(1900, "Unknown");

        private final int code;
        private final String description;
        Status(int code, String description) { this.code = code; this.description = description; }
        public int code() { return code; }
        @JsonValue public String description() { return description; }
    }

    /** reasonCode 是跨 provider 穩定數字；reason 只提供英文人類可讀說明。 */
    enum Reason {
        FILLED_AT_REFERENCE(10000, "Filled at reference price"),
        VENUE_ACK_DELAYED(10010, "Venue acknowledgement delayed"),
        DELAYED_FILL_CONFIRMED(10011, "Delayed fill confirmed"),
        PARTIAL_LIQUIDITY(10100, "Only partial liquidity was available"),
        FILLED_WITHIN_SLIPPAGE_LIMIT(10200, "Filled within slippage limit"),
        SLIPPAGE_LIMIT_EXCEEDED(14200, "Slippage limit exceeded"),
        VENUE_REJECTED_ORDER(14300, "Venue rejected order"),
        VENUE_OUTCOME_UNKNOWN(19000, "Venue outcome unknown"),
        ORDER_NOT_FOUND(19404, "External order was not found"),
        QUANTITY_TOO_SMALL(14500, "Quantity was too small to fill");

        private final int code;
        private final String description;
        Reason(int code, String description) { this.code = code; this.description = description; }
        public int code() { return code; }
        @JsonValue public String description() { return description; }
    }

    /** reasonCode 與均價採 provider-neutral 語意，讓上層不依賴 Binance 或其他交易所欄位。 */
    record Outcome(String clientOrderId, String externalOrderId, Status status, int statusCode,
                   BigDecimal filledQuantity, BigDecimal filledNotional,
                   BigDecimal averageFillPrice, Reason reason, int reasonCode) {
        public Outcome {
            Objects.requireNonNull(status, "external order status is required");
            Objects.requireNonNull(reason, "external order reason is required");
            if (statusCode != status.code() || reasonCode != reason.code())
                throw new IllegalArgumentException("external order numeric code does not match description");
        }
    }
}
