/**
 * P21-T05 的唯讀 trade、ticker 與 candle 聚合模型。
 *
 * <p>此 package 僅接收已正規化且已受理的行情事件，沒有 provider、transport、cache、database 或交易核心
 * 依賴。所有 transition 均由呼叫端傳入前一 immutable projection，以維持 deterministic replay。</p>
 */
package com.lumix.marketdata.aggregation;
