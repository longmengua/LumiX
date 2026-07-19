/**
 * Phase 18-T06 的受限 futures contract sandbox eligibility boundary。
 *
 * 此 package 只鎖定單一 market 的 accepted order 與人工 mark-price input；不執行 matching、fill、
 * position mutation、PnL/funding 套用、margin reservation、balance/ledger mutation 或 settlement。
 */
package com.lumix.trading.core.futures.sandbox.contract;
