/**
 * ledger domain layer 的入口 package。
 *
 * 這一層預留給 immutable ledger invariants、journal draft、domain policy 與 value object。
 * 現階段只建立邊界，不把 posting 或 reconciliation runtime 放進來；
 * 任何把這裡的 contract 接成正式資金路徑的變更都屬於 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.domain;
