/**
 * ledger bounded context 的入口 package。
 *
 * 帳本規則必須維持 immutable append-only，因此此處只先建立邊界標記，
 * 不在 Phase 13 內加入 posting 或 reversal 實作。
 */
package com.lumix.ledger;
