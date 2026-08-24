# P25-T02 Signing Intent 邊界

## 範圍

將 P24 keyless input 與 P25-T01 approved evidence 綁定成 deterministic signing-intent digest 與 request-to-sign idempotency contract，讓後續 signer 能在不重解釋 request 的前提下驗證 input。

## 不變式

- digest 必須綁定 request、hold、audit digest、asset、network、destination 與 atomic amount。
- 未通過 P25-T01 的 approval result 一律不可建立 intent。
- 同 request/idempotency key 只有完全相同的 intent 可重放；不同 payload 必須拒絕。
- 此 task 不產生 transaction serialization、nonce、UTXO、gas、私鑰、簽名或 signer command。

## 風險

`HUMAN_REVIEW_REQUIRED: yes`；審查 digest canonicalization、approval 的輸入來源與 idempotency scope。
