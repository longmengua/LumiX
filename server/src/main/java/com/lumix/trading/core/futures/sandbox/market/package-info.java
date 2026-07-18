/**
 * Phase 18-T05 的 mock mark-price sandbox 邊界。
 *
 * 此 package 只接受人工提供的 immutable price snapshot，並受限地轉交 T04 PnL valuation；
 * 不連接 production market service、不保存行情，也不更新 position、balance、ledger 或 settlement。
 */
package com.lumix.trading.core.futures.sandbox.market;
