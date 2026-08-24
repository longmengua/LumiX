/**
 * P23-T04 的 deposit-to-ledger-to-balance 唯讀 reconciliation 與 audit export input 契約。
 *
 * <p>此套件只比對 caller 提供的 immutable evidence，產生 exception report；它不查詢或修復 ledger/balance，也沒有 admin command。</p>
 */
package com.lumix.deposit.credit.reconciliation;
