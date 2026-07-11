/**
 * ledger append persistence adapter 的 package。
 *
 * 這一層只保留最小 DB append gate，不是 posting service，也不是完整 transaction orchestration。
 * 任何正式接到 runtime 的變更都屬於 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.persistence.adapter;
