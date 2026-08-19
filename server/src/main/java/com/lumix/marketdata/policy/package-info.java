/**
 * P21-T03 的唯讀 stream admission 與 feed health policy。
 *
 * <p>package 不含 provider、transport、scheduler、projection 或持久化；它只將明確輸入的 event、
 * cursor 與 evaluation timestamp 轉為 deterministic decision。</p>
 */
package com.lumix.marketdata.policy;
