/**
 * ledger persistence layer 的入口 package。
 *
 * 這一層只預留 repository contract 與讀寫分離的持久層邊界。
 * Phase 14-T03 仍只保留 append-only mapping contract，不在此實作任何 ledger posting、
 * database write 或 balance mutation；所有相關變更都屬於 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.persistence;
