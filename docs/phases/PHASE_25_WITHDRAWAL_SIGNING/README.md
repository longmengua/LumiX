# Phase 25 - 提款審核、簽章、廣播

## 狀態

```text
COMPLETED_FOR_WITHDRAWAL_SIGNING_BOUNDARY_FOUNDATION
```

## Phase charter

以權限分離、最小授權與可對帳為前提，將 approved withdrawal 從 approval decision 安全地交給 signer 與 broadcaster。P25 已完成 immutable boundary foundation；所有 signer runtime、secret、HSM/MPC、鏈上連線與 broadcast 均未施工。

## 中階 task breakdown

1. P25-T01 審核政策與職責分離：雙人/多角色 approval、限額、expiry、拒絕與 escalation；禁止 self-approval 與 bypass。`COMPLETED`，詳見 `p25-t01-approval-separation.md`。
2. P25-T02 Signing intent boundary：固定 transaction intent、destination/amount/network binding、nonce/UTXO selection evidence、request-to-sign idempotency；私鑰永不進 application log、DB 或一般 runtime。`COMPLETED`，詳見 `p25-t02-signing-intent-boundary.md`。
3. P25-T03 Signer adapter isolation：HSM/MPC/外部 signer capability、secret lifecycle、timeout/retry、failure isolation；每一 signer/provider 需獨立 task card。`COMPLETED`；目前僅有 capability / dispatch envelope contract，沒有 adapter。
4. P25-T04 Broadcast 與確認對帳：broadcast identity、retry、pending/replaced/failed、chain confirmation、ledger/balance/reservation completion handoff；不得重複出金。`COMPLETED`，僅有 read-only evidence contract。
5. P25-T05 Security/audit review：approval、sign、broadcast 全鏈 immutable audit evidence、異常人工處置與 recovery drill。`COMPLETED_FOR_FOUNDATION`，完整 runtime drill 屬後續營運 phase。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。任何 task 要先證明 destination/amount 未被竄改、權限不可繞過、簽章不可重放、broadcast 可唯一追蹤、失敗不會自動釋放或重複支出。逐卡 approve 暫停期間仍須保留此標記與 fail-closed 邊界；P25-T01 不使用 signer、secret 或鏈上 runtime。

## P25-T01 實作紀錄

`com.lumix.withdrawal.approval` 定義 caller 提供的 immutable reviewer/role/authorization-evidence-version、角色 requirement、限額與 expiry。只有 P24 已 handoff 且 hold identity 相符的 keyless input，並由非 request owner 的不同 reviewer 覆蓋所有 required roles 時才得到 `APPROVED`；任何 self-approval、重複 reviewer、角色不足、超限或過期都 fail-closed。此結果不是實際角色授與、帳號 authorization、簽章或 broadcast 命令。

## P25-T02/T03 實作紀錄

`com.lumix.withdrawal.signing` 將 request、hold、audit digest、asset、network、destination 與 atomic amount 固定為 SHA-256 intent digest；未批准或同 request/key 不同 payload 都不能建立 intent。capability policy 另以 network allowlist、availability 和明確 timeout/retry 限制產生 adapter 前的純資料封套；封套沒有 private key、signature、transaction serialization 或 adapter invocation。

## P25-T04/T05 實作紀錄

`com.lumix.withdrawal.broadcast` 只驗證外部提供的 attempt identity、intent digest、reference、觀測時間與 broadcast/confirmation status。digest 不符、attempt 重複、terminal 後回退或未 confirmed 都 fail-closed；`FAILED` 不帶任何自動 release/retry 語意。P25 全鏈採 P24 audit digest、approval evidence、signing intent digest 與 broadcast evidence 的 immutable input/output，沒有可執行 signer、broadcast、ledger 或 balance path。
