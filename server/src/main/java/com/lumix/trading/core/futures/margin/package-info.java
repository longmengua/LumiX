/**
 * Futures isolated margin sandbox gate。
 *
 * 這個 package 只放單一 proposed position 的 initial-margin sufficiency gate，刻意保持 pure、stateless、
 * deterministic，且不接 balance lookup、reservation、matching、settlement、liquidation、funding 或 persistence。
 */
package com.lumix.trading.core.futures.margin;
