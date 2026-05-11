package com.example.exchange.application.usecase;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.service.OrderService;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.TimeInForce;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PlaceOrderUseCase（下單用例）
 * -------------------------------------------------
 *
 * 角色定位：
 * - 此類別位於 Application UseCase 層，負責接收外部下單指令（PlaceOrderCommand），
 *   將其轉換為領域模型 Order，再交由 OrderService 進一步處理。
 *
 * 職責範圍：
 * 1) 驗證下單請求中的基本欄位
 * 2) 將 symbol 字串轉換成 Symbol 值物件
 * 3) 解析 TimeInForce
 * 4) 根據訂單型別決定價格欄位的處理方式
 * 5) 建立 Order 聚合
 * 6) 呼叫 OrderService 執行後續撮合流程
 *
 * 設計說明：
 * - LIMIT：
 *   - 必須由使用者提供正數價格
 *   - 後續可依價格條件決定是否掛簿
 *
 * - MARKET：
 *   - 不帶價格（price = null）
 *   - 只吃當前訂單簿上的對手盤
 *   - 若流動性不足，剩餘量直接失效
 *   - 不應進入訂單簿
 *
 * 注意：
 * - 此類別目前僅負責「下單資料轉換與基本檢查」
 * - 真正的風控、保證金、資金凍結、reduceOnly 驗證等，應由後續服務處理
 */
@Component
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    /**
     * 預設報價幣
     * - 目前簡化設計只接受 USDT 作為 quote asset
     * - 例如：BTCUSDT、ETHUSDT
     */
    private static final String DEFAULT_QUOTE = "USDT";

    /**
     * 訂單服務
     * - 負責真正的撮合、事件寫入、倉位更新、訂單狀態回寫
     */
    private final OrderService orderService;

    /**
     * 處理下單請求
     * -------------------------------------------------
     * 流程：
     * 1) 驗證基本欄位
     * 2) 解析交易對
     * 3) 解析 TimeInForce
     * 4) 決定 price（LIMIT 必填；MARKET 為 null）
     * 5) 建立 Order 聚合
     * 6) 交由 OrderService 處理
     *
     * @param cmd 下單指令
     */
    public void handle(PlaceOrderCommand cmd) {
        // 1) 基本欄位檢查
        validateCommand(cmd);

        // 2) 解析交易對
        String code = cmd.symbol().trim().toUpperCase();

        // TODO: 目前只接受 USDT quote；若未來支援其他 quote，應改由 symbol config 查表
        String base = code.substring(0, code.length() - DEFAULT_QUOTE.length());

        Symbol symbol = Symbol.builder()
                .base(base)
                .quote(DEFAULT_QUOTE)
                .priceScale(2)
                .qtyScale(3)
                .build();

        // 3) 解析訂單有效期
        TimeInForce tif = parseTimeInForce(cmd.timeInForce());

        // 4) 決定實際寫入訂單的價格
        BigDecimal price = resolveOrderPrice(cmd);

        // ===== TODO: 風控 & 資金前置檢查 =====
        // TODO: 檢查交易對是否允許交易（maintenance / only-reduce 模式）
        // TODO: 檢查用戶交易許可（KYC、風險標記、黑名單、IP / 國別合規）
        // TODO: 檢查下單尺寸限制（最小 / 最大 qty、最小名義金額、步進刻度）
        // TODO: 檢查帳戶模式 / 槓桿（CROSS / ISOLATED、leverage 區間）
        // TODO: 試算初始保證金 IM = notional / leverage（合約）或 = notional（現貨借貸則不同）
        // TODO: 試算 taker 可能成交手續費上限（預凍結備用）
        // TODO: 檢查可用餘額 >= (IM + 預估手續費 + 其他預留)，不足則拒單
        // TODO: 凍結資金：凍結 IM 與預估手續費上限（返回 freezeId 供後續釋放/調整）

        /**
         * 建立訂單聚合
         * -------------------------------------------------
         * 注意：
         * - qty         = 剩餘量
         * - origQty     = 原始下單量
         * - executedQty = 初始為 0
         * - avgPrice    = 初始為 0
         */
        Order order = Order.builder()
                .uid(cmd.uid())
                .symbol(symbol)
                .side(cmd.side())
                .type(cmd.type())
                .price(price)                     // LIMIT 有價格；MARKET 為 null
                .qty(cmd.qty())                  // 剩餘量
                .origQty(cmd.qty())              // 原始量
                .executedQty(BigDecimal.ZERO)    // 已成交量
                .avgPrice(BigDecimal.ZERO)       // 平均成交價
                .timeInForce(tif)
                .reduceOnly(cmd.reduceOnly())
                .clientOrderId(cmd.clientOrderId())
                // TODO: order.attachFreezeId(freezeId)   // 綁定凍結資金憑證
                // TODO: order.setLeverage(leverage)      // 記錄下單時的槓桿
                // TODO: order.setMarginMode(...)         // 記錄下單時的保證金模式
                .build();

        // TODO: 記錄審計/埋點（風控審核流水、埋點追蹤等）

        // 5) 交由訂單服務處理
        orderService.processOrder(order);
    }

    /**
     * 驗證下單指令的基本欄位
     * -------------------------------------------------
     * 目前只做最基本的防守性檢查，避免明顯非法輸入：
     * - command 不可為 null
     * - symbol 不可為空
     * - 目前僅支援 USDT quote
     * - qty 必須為正數
     * - side / type 不可為 null
     *
     * @param cmd 下單指令
     */
    private void validateCommand(PlaceOrderCommand cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("place order command cannot be null");
        }

        if (cmd.symbol() == null || cmd.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol cannot be blank");
        }

        String symbol = cmd.symbol().trim().toUpperCase();
        if (!symbol.endsWith(DEFAULT_QUOTE)) {
            throw new IllegalArgumentException("only USDT quote symbol is supported for now");
        }

        if (symbol.length() <= DEFAULT_QUOTE.length()) {
            throw new IllegalArgumentException("invalid symbol format: " + cmd.symbol());
        }

        if (cmd.qty() == null || cmd.qty().signum() <= 0) {
            throw new IllegalArgumentException("qty must be greater than zero");
        }

        if (cmd.type() == null) {
            throw new IllegalArgumentException("order type cannot be null");
        }

        if (cmd.side() == null) {
            throw new IllegalArgumentException("order side cannot be null");
        }
    }

    /**
     * 解析 TimeInForce
     * -------------------------------------------------
     * 規則：
     * - 若未提供，預設為 GTC
     * - 若提供非法值，拋出 IllegalArgumentException
     *
     * @param rawTimeInForce 原始字串
     * @return 解析後的 TimeInForce
     */
    private TimeInForce parseTimeInForce(String rawTimeInForce) {
        if (rawTimeInForce == null || rawTimeInForce.isBlank()) {
            return TimeInForce.GTC;
        }

        try {
            return TimeInForce.valueOf(rawTimeInForce.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported timeInForce: " + rawTimeInForce);
        }
    }

    /**
     * 根據訂單型別決定實際寫入訂單的價格
     * -------------------------------------------------
     * 規則：
     * - LIMIT：必須提供正數價格
     * - MARKET：不應帶價格，直接回傳 null
     *
     * 注意：
     * - 正統市場單不應依賴人造極端價格
     * - 是否能成交，應由撮合引擎直接依對手簿流動性決定
     *
     * @param cmd 下單指令
     * @return 實際寫入 Order 的價格；MARKET 會回傳 null
     */
    private BigDecimal resolveOrderPrice(PlaceOrderCommand cmd) {
        if (cmd.type() == OrderType.MARKET) {
            return null;
        }

        BigDecimal price = cmd.price();
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("LIMIT order requires positive price");
        }

        return price;
    }
}