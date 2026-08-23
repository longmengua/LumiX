/**
 * P21-T04 的唯讀 order-book projection reducer。
 *
 * <p>只消費 T02 normalized book event 與 T03 admission decision；不依賴 sandbox order book、matching、
 * provider、transport 或持久化，亦不宣稱任何 projection 是公開或正式交易流動性。</p>
 */
package com.lumix.marketdata.book;
