/**
 * Trading Runtime Core 的設計 package。
 *
 * 這裡只放 scope gate 與 safety contract 的設計型別，不放正式 money movement runtime。
 * Phase 15-T01 只建立門檻，不把任何 trading path 接到實際 DB 寫入流程。
 */
package com.lumix.trading.core;
