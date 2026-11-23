package com.lumix.trading.liquidity;

import com.lumix.trading.runtime.TradingInstrumentRules;
import java.math.BigDecimal;

/**
 * 站內互撮後剩餘量的外部送單邊界。來源行情不實作此介面；只有具交易權限、可查單並可對帳的 adapter 才能接入。
 * 客戶成交量只可依站內或外部確定成交回報入帳；逾時或回報不明時查詢原命令，不得自行宣告成交。
 */
public interface ExternalOrderRouter {
    /** 以穩定的 clientOrderId 送單；結果只描述外部狀態，不代表 LumiX 帳本已完成結算。 */
    Outcome submit(Intent intent);

    /** 依原 clientOrderId 查詢外部訂單，供逾時及程序重啟後恢復。 */
    Outcome lookup(String source, String clientOrderId);

    record Intent(String source, String clientOrderId, TradingInstrumentRules.Kind kind,
                  String marketSymbol, Side side, BigDecimal quantity, BigDecimal limitPrice,
                  TimeInForce timeInForce) { }

    enum Side { BUY, SELL }

    /** 外部剩餘量的 IOC／FOK 選擇由客戶命令與已核定政策決定。 */
    enum TimeInForce { IOC, FOK }

    enum Status { PENDING, PARTIALLY_FILLED, FILLED, REJECTED, CANCELED, UNKNOWN }

    record Outcome(String clientOrderId, String externalOrderId, Status status,
                   BigDecimal filledQuantity, BigDecimal filledNotional) { }
}
