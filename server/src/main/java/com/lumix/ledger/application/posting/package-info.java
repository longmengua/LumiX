/**
 * ledger posting command boundary 的 application package。
 *
 * 這一層只放 command、result、plan 與 boundary contract。
 * Phase 14-T04 仍然不能進入正式寫入或任何 balance mutation；所有相關變更都屬於 HUMAN_REVIEW_REQUIRED。
 */
package com.lumix.ledger.application.posting;
