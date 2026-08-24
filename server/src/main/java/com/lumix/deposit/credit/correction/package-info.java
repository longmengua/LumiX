/**
 * P23-T03 的 reorg 後 freeze、append-only correction 與人工升級 decision 契約。
 *
 * <p>此套件不呼叫 ledger、也不刪改任何歷史 record；它只把後續允許的修正方向明確化。</p>
 */
package com.lumix.deposit.credit.correction;
