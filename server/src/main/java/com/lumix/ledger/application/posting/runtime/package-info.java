/**
 * 受控 ledger posting runtime 接線 package。
 *
 * 這裡只放 Phase 15 的最小受控接線門檻，不放完整 trading runtime、balance projection runtime 或 settlement runtime。
 * 任何把這裡誤接成 production-ready money movement 的變更，都屬於 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.application.posting.runtime;
