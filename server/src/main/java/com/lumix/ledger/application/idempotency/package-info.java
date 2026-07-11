/**
 * ledger idempotency design 的 application package。
 *
 * 這個 package 只放 request identity 與 idempotency contract 的設計型別，不放任何正式 runtime、DB lookup 或 lock 實作。
 * Phase 14-T08 只建立設計門檻，所有正式 idempotency runtime 都屬於 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.application.idempotency;
