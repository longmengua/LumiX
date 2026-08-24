/**
 * P22-T03 的 confirmation、reorg 與觀測健康狀態契約。
 *
 * <p>所有類別皆為 pure state transition；finality threshold 不是帳本 credit 授權，任何 halted/recovery 決定也不會
 * 連線 provider 或修改 wallet、balance、ledger。</p>
 */
package com.lumix.deposit.observation.finality;
