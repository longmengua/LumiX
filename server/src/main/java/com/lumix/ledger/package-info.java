/**
 * ledger bounded context 的入口 package。
 *
 * 帳本規則必須維持 immutable append-only，因此此處只建立邊界標記與 prerequisite gate，
 * 不在 Phase 14-T01 內加入 posting、reversal 或 balance mutation 實作。
 */
package com.lumix.ledger;
