/**
 * P21-T06 的 deterministic replay、resync 與 recovery 邊界。
 *
 * <p>所有 event、初始 state 與 evaluation timestamp 都由呼叫端明確傳入；此 package 不含網路、provider、
 * scheduler、event store 或任何自動 recovery side effect。</p>
 */
package com.lumix.marketdata.replay;
