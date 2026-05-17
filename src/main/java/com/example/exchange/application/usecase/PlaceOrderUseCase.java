/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.service.OrderService;
import com.example.exchange.application.service.RiskService;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.model.enums.TimeInForce;
import com.example.exchange.domain.repository.SymbolConfigRepository;
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
 * - 此類別負責「下單資料轉換與基本檢查」
 * - 交易規則、風控、保證金預凍與 reduceOnly 驗證交給 RiskService 處理
 */
@Component
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    /**
     * 訂單服務
     * - 負責真正的撮合、事件寫入、倉位更新、訂單狀態回寫
     */
    private final OrderService orderService;
    private final RiskService riskService;
    private final SymbolConfigRepository symbolConfigRepository;

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
        SymbolConfig symbolConfig = symbolConfigRepository.findBySymbol(code)
                .orElseThrow(() -> new IllegalArgumentException("unsupported symbol: " + cmd.symbol()));
        Symbol symbol = symbolConfig.toSymbol();

        // 3) 解析訂單有效期
        TimeInForce tif = parseTimeInForce(cmd.timeInForce());
        MarginMode marginMode = parseMarginMode(cmd.marginMode());

        // 4) 決定實際寫入訂單的價格
        BigDecimal price = resolveOrderPrice(cmd);

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
                .postOnly(cmd.postOnly())
                .leverage(cmd.leverage())
                .marginMode(marginMode)
                .clientOrderId(cmd.clientOrderId())
                .build();

        // 送入撮合前完成 symbol config、槓桿/倉位/價格偏離與可用餘額檢查，並寫入委託凍結流水。
        riskService.preCheckAndReserve(order, symbolConfig);

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

        if (cmd.qty() == null || cmd.qty().signum() <= 0) {
            throw new IllegalArgumentException("qty must be greater than zero");
        }

        if (cmd.type() == null) {
            throw new IllegalArgumentException("order type cannot be null");
        }

        if (cmd.side() == null) {
            throw new IllegalArgumentException("order side cannot be null");
        }

        if (cmd.leverage() <= 0) {
            throw new IllegalArgumentException("leverage must be greater than zero");
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

    private MarginMode parseMarginMode(String rawMarginMode) {
        if (rawMarginMode == null || rawMarginMode.isBlank()) {
            return MarginMode.CROSS;
        }

        try {
            return MarginMode.valueOf(rawMarginMode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported marginMode: " + rawMarginMode);
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
