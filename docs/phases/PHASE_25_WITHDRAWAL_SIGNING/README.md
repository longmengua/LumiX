# Phase 25 - 提款審核、簽章、廣播

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

以權限分離、最小授權與可對帳為前提，將 approved withdrawal 從 approval decision 安全地交給 signer 與 broadcaster。所有 runtime、secret、HSM/MPC、鏈上連線均未獲批准。

## 中階 task breakdown

1. 審核政策與職責分離：雙人/多角色 approval、限額、expiry、拒絕與 escalation；禁止 self-approval 與 bypass。
2. Signing intent boundary：固定 transaction intent、destination/amount/network binding、nonce/UTXO selection evidence、request-to-sign idempotency；私鑰永不進 application log、DB 或一般 runtime。
3. Signer adapter isolation：HSM/MPC/外部 signer capability、secret lifecycle、timeout/retry、failure isolation；每一 signer/provider 需獨立 task card。
4. Broadcast 與確認對帳：broadcast identity、retry、pending/replaced/failed、chain confirmation、ledger/balance/reservation completion handoff；不得重複出金。
5. Security/audit review：approval、sign、broadcast 全鏈 immutable audit evidence、異常人工處置與 recovery drill。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。任何 task 要先證明 destination/amount 未被竄改、權限不可繞過、簽章不可重放、broadcast 可唯一追蹤、失敗不會自動釋放或重複支出。未完成 P24 review、signer threat model、secret policy 與 reconciliation contract 時停止。
