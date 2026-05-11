package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.enums.TimeInForce;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Order（訂單聚合）
 * -------------------------------------------------
 *
 * 角色定位：
 * - 此類別是交易系統中的核心領域模型之一，表示一張委託單。
 * - 一張 Order 會描述：誰下單、下哪個交易對、買或賣、限價或市價、委託量、成交狀態等資訊。
 * - 撮合引擎（Matching Engine）會直接讀寫此物件，並在成交時更新其剩餘量與狀態。
 *
 * 設計重點：
 * 1) 使用 Lombok @Data 簡化 getter/setter/toString/equals/hashCode 樣板碼
 * 2) 使用 @Builder + @Jacksonized，讓 Jackson 可透過 Builder 做 JSON 反序列化
 * 3) 使用 @Builder.Default 提供欄位預設值，避免 builder 未指定時為 null
 * 4) qty 表示「剩餘未成交數量」
 * 5) origQty 表示「原始委託量」
 * 6) executedQty 表示「已成交數量」
 * 7) avgPrice 表示「目前累積成交均價」
 *
 * 生命週期（簡化）：
 * - 建單時：status = NEW
 * - 部分成交：status = PARTIALLY_FILLED
 * - 全部成交：status = FILLED
 * - 主動撤單：status = CANCELED
 * - 拒絕委託：status = REJECTED
 * - 逾期失效（例如 IOC/FOK 殘量）：status = EXPIRED
 *
 * 注意：
 * - 本類別目前額外提供 toOrderInfoResponse()，方便直接轉成 API DTO
 * - 但嚴格分層來說，Domain 不應依賴 Web DTO；後續建議抽成 Mapper
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    /**
     * 訂單狀態列舉
     * -------------------------------------------------
     * NEW              : 新建單，尚未成交
     * PARTIALLY_FILLED : 部分成交
     * FILLED           : 全部成交
     * CANCELED         : 已撤單
     * REJECTED         : 拒單（例如風控失敗、參數不合法）
     * EXPIRED          : 已失效（例如 IOC/FOK 未完全成交）
     */
    public enum Status {
        NEW,
        PARTIALLY_FILLED,
        FILLED,
        CANCELED,
        REJECTED,
        EXPIRED
    }

    /**
     * 訂單唯一識別碼
     * - 預設使用 UUID 自動產生
     * - 在真實交易所中，也可能改由雪花 ID / 資料庫流水號 / 撮合引擎序號產生
     */
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /**
     * 使用者 ID
     * - 表示是哪位使用者提交了這張訂單
     */
    private long uid;

    /**
     * 交易對
     * - 例如 BTCUSDT
     * - 使用 Symbol 值物件承載 base/quote 與精度資訊
     */
    private Symbol symbol;

    /**
     * 下單方向
     * - BUY = 買入 / 做多
     * - SELL = 賣出 / 做空
     */
    private OrderSide side;

    /**
     * 訂單型別
     * - LIMIT  = 限價單
     * - MARKET = 市價單
     */
    private OrderType type;

    /**
     * 委託價格
     * - LIMIT：通常必填
     * - MARKET：可為 null；本專案目前以極端價格模擬市價單撮合
     */
    private BigDecimal price;

    /**
     * 剩餘數量（remaining quantity）
     * - 表示當前尚未成交的部分
     * - 每次成交後會遞減
     * - 當 qty == 0 時，代表此訂單已全部成交
     */
    private BigDecimal qty;

    /**
     * 原始委託數量（original quantity）
     * - 建單時通常等於最初傳入的 qty
     * - 不會隨撮合遞減，用於查詢與前端顯示
     */
    @Builder.Default
    private BigDecimal origQty = BigDecimal.ZERO;

    /**
     * 已成交數量（executed quantity）
     * - 每次撮合成交後累加
     * - executedQty + qty 通常應等於 origQty
     */
    @Builder.Default
    private BigDecimal executedQty = BigDecimal.ZERO;

    /**
     * 平均成交價（average executed price）
     * - 若尚未成交，通常為 0
     * - 每次 fill 後，依加權平均方式更新
     */
    @Builder.Default
    private BigDecimal avgPrice = BigDecimal.ZERO;

    /**
     * 訂單有效期（Time In Force）
     * - GTC：Good Till Cancel，未成交部分繼續掛簿直到取消
     * - IOC：Immediate Or Cancel，立即成交能吃到的部分，其餘失效
     * - FOK：Fill Or Kill，必須全部成交，否則整張失效
     */
    @Builder.Default
    private TimeInForce timeInForce = TimeInForce.GTC;

    /**
     * 是否為 reduce-only 訂單
     * - true  表示只能減少既有倉位，不可增加倉位
     * - false 表示一般委託
     */
    @Builder.Default
    private boolean reduceOnly = false;

    /**
     * 客戶端自定義訂單 ID
     * - 通常用來做冪等控制或讓前端/策略端對帳
     * - 例如 strategy-20260319-0001
     */
    private String clientOrderId;

    /**
     * 拒單原因碼
     * - 僅在 status = REJECTED 時有意義
     * - 例如：MARGIN_INSUFF、INVALID_PRICE、REDUCE_ONLY_VIOLATION
     */
    private String rejectCode;

    /**
     * 訂單狀態
     * - 預設為 NEW
     */
    @Builder.Default
    private Status status = Status.NEW;

    /**
     * 建立時間
     * - 預設為目前 UTC 時間
     */
    @Builder.Default
    private Instant ctime = Instant.now();

    /**
     * 轉換成 API 回傳 DTO
     * -------------------------------------------------
     * 用途：
     * - 方便 Controller / API 層直接返回前端所需資料
     *
     * 注意：
     * - 這個方法會讓 Domain 依賴到 Web DTO
     * - 在乾淨分層設計中，建議之後搬到 Mapper 類別處理
     *
     * @return OrderInfoResponse API 回應物件
     */
    public OrderInfoResponse toOrderInfoResponse() {
        return new OrderInfoResponse(
                getId().toString(),
                getUid(),
                getSymbol().code(),
                getSide(),
                getType(),
                getPrice(),
                getOrigQty(),
                getQty(),
                getExecutedQty(),
                getAvgPrice(),
                getTimeInForce().name(),
                isReduceOnly(),
                getClientOrderId(),
                getStatus().name(),
                getRejectCode(),
                getCtime()
        );
    }

    /**
     * 套用一次成交
     * -------------------------------------------------
     * 功能：
     * - 更新剩餘量 qty
     * - 更新已成交量 executedQty
     * - 更新平均成交價 avgPrice
     * - 依剩餘量決定狀態是 PARTIALLY_FILLED 或 FILLED
     *
     * 規則：
     * - execQty 必須為正數
     * - execPrice 必須為正數
     * - avgPrice 以加權平均方式計算：
     *
     *   新均價 =
     *   (舊均價 * 舊已成交量 + 本次成交價 * 本次成交量)
     *   / 新已成交總量
     *
     * @param execQty   本次成交數量（正數）
     * @param execPrice 本次成交價格（正數）
     */
    public void fill(BigDecimal execQty, BigDecimal execPrice) {
        // 防守式檢查：成交量為 null 或 <= 0，直接忽略
        if (execQty == null || execQty.signum() <= 0) return;

        // 防守式檢查：成交價為 null 或 <= 0，直接忽略
        if (execPrice == null || execPrice.signum() <= 0) return;

        // 舊的已成交總名義金額 = 舊均價 * 舊已成交量
        BigDecimal filledNotional = this.avgPrice.multiply(this.executedQty);

        // 本次成交名義金額 = 本次成交價 * 本次成交量
        BigDecimal newNotional = execPrice.multiply(execQty);

        // 新的已成交總量
        BigDecimal totalExecuted = this.executedQty.add(execQty);

        // 扣減剩餘量
        this.qty = this.qty.subtract(execQty);

        // 更新已成交量
        this.executedQty = totalExecuted;

        // 重新計算加權平均成交價
        if (totalExecuted.signum() > 0) {
            this.avgPrice = filledNotional.add(newNotional)
                    .divide(totalExecuted, 8, RoundingMode.HALF_UP);
        }

        // 根據剩餘量決定狀態
        if (this.qty.signum() <= 0) {
            this.qty = BigDecimal.ZERO;
            this.status = Status.FILLED;
        } else {
            this.status = Status.PARTIALLY_FILLED;
        }
    }

    /**
     * 將訂單標記為已撤單
     * -------------------------------------------------
     * 用途：
     * - 使用者主動取消掛單
     * - 系統撤單
     *
     * 注意：
     * - 呼叫前通常應先確認此訂單仍可撤（例如不是 FILLED / CANCELED）
     */
    public void cancel() {
        this.status = Status.CANCELED;
    }

    /**
     * 將訂單標記為失效
     * -------------------------------------------------
     * 常見情境：
     * - IOC 未成交完的剩餘量作廢
     * - FOK 無法一次全部成交時整張作廢
     */
    public void expire() {
        this.status = Status.EXPIRED;
    }

    /**
     * 將訂單標記為拒絕
     * -------------------------------------------------
     * 常見情境：
     * - 風控拒單
     * - 保證金不足
     * - reduceOnly 違規
     * - 參數格式不合法
     *
     * @param code 拒絕原因碼
     */
    public void reject(String code) {
        this.status = Status.REJECTED;
        this.rejectCode = code;
    }
}