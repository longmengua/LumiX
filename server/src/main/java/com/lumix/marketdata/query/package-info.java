/**
 * P21-T07 的 internal-only 唯讀 query/stream contract 與 pure backpressure policy。
 *
 * <p>這裡不是 HTTP、WebSocket、broker 或 authentication runtime；所有輸出都以 immutable envelope 表達，
 * 讓未來獲批准的 transport 無法省略 health、sequence 或 resnapshot 語意。</p>
 */
package com.lumix.marketdata.query;
