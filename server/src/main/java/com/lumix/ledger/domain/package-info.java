/**
 * ledger domain layer 的入口 package。
 *
 * 這一層預留給 immutable ledger invariants、domain policy 與 value object。
 * 現階段只建立邊界，不把 posting 或 reconciliation runtime 放進來。
 */
package com.lumix.ledger.domain;
