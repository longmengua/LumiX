/**
 * Phase 19-T01 的 futures liquidation pure simulation boundary。
 *
 * 此 package 只以人工 collateral、mock mark price 與 maintenance rate 計算門檻結果；
 * 不執行強平、position close、balance/ledger mutation、reservation、settlement 或正式風控控制。
 */
package com.lumix.trading.core.futures.sandbox.liquidation;
