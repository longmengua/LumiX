/**
 * ledger append transaction boundary 的 application package。
 *
 * 這一層只保留 transaction design、marker 與 policy，不執行任何 DB 寫入。
 * 所有正式 append runtime 仍屬 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.application.transaction;
