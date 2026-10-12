# Withdrawal Runtime E2E Implementation Report

## Executive Summary

狀態：`PARTIAL`。已在 `lumix-server` 建立可執行的提款 runtime 與獨立 Sepolia signer boundary；但 live Sepolia ETH E2E 尚未執行，P25 approval policy 尚未能直接消費 runtime DB evidence，故不得宣稱 Goal COMPLETE 或 production ready。

## Implemented Components

- `POST/GET /api/v1/withdrawals`、取消 API、使用者地址簿 API。
- `withdrawals` runtime state machine、durable create idempotency、reservation hold/release/capture。
- address cooldown、immutable request snapshot、risk assessment/trigger persistence、Redis velocity。
- transaction-specific authorization：有效、未撤銷、未過期且未超過受控 age 的 authenticated session；沒有偽裝為 2FA。
- admin review queue、owner/reviewer separation、approval payload hash 與 final risk gate。
- signing intent、獨立 signer app、EIP-1559 ETH 與 ERC-20 USDT builder、persistent nonce allocation、broadcast 與 finality reconciliation。
- confirmed 後使用既有 immutable ledger posting service 完成結算；audit/outbox evidence 在重大 state change 寫入。
- Micrometer 暴露低基數 request、risk、review/sign/chain pending、signed、broadcast、completed、failed、RPC/signer/Redis error metrics。

## Files Changed

主要 runtime 位於 `server/src/main/java/com/lumix/withdrawal/`；schema 位於 `server/src/main/resources/db/migration/V002__withdrawal_platform_runtime.sql`；獨立 signer 位於 `server/signer/`。

## Database Migrations

V002 擴充既有 `withdrawals`，新增 address、risk、review、signing、chain transaction 與 nonce evidence tables。V003 再擴充同一 runtime：`BROADCAST_UNKNOWN`、nonce allocation lifecycle/txHash 以及 gas 收據證據。所有提款附屬資料以 foreign key 指向既有提款主表。

## Withdrawal State Machine

```text
REQUESTED -> FUNDS_HELD -> RISK_PENDING
  -> MANUAL_REVIEW / APPROVED / RISK_HOLD / REJECTED
APPROVED -> FINAL_RISK_CHECK -> SIGN_PENDING -> SIGNING -> SIGNED
  -> BROADCASTING -> BROADCAST_UNKNOWN / BROADCASTED -> CONFIRMING -> COMPLETED
```

非法倒退 transition 會被集中 state machine 拒絕。

## Request / Reservation Flow

create 先 durable claim idempotency key，再建立 snapshot，並以既有 locked balance/reservation runtime 建立 `WITHDRAWAL_HOLD`。拒絕與取消在 broadcast 前以 idempotent release 解鎖。receipt finalized 後才 capture reservation 並 post ledger。

## Risk Rules

已接入 account withdrawal freeze、transfer restriction、recent password change、new device、新 IP、高額、address active/cooldown 與 Redis unavailable fail-safe。

## Velocity Rules

Redis key 追蹤 user 10m/1h/24h count、24h amount、device 1h count 與 hashed IP 1h count。Redis 非 financial source of truth；不可用時為 MANUAL_REVIEW。

## Address Book

Ethereum address 會 server-side validate/normalize；地址新增為 COOLDOWN，讀取或提款驗證時才安全轉 ACTIVE；刪除改為 DISABLED 並保留 audit history。

## Manual Review

admin review 使用現有 server-side super-admin gate。request owner 不得 reviewer；同 reviewer 的重覆 review 由 database uniqueness 拒絕。approve 的 canonical payload hash 於送簽前重新驗證。

## Maker Checker

已實作 requester/reviewer separation。金額達到 `lumix.withdrawal.platform.dual-review-amount` 時，第一位 reviewer 的 approval 保持 `MANUAL_REVIEW`，必須由第二位不同 super-admin 對相同 payload hash 核准才可進 final risk；資料庫唯一鍵拒絕同 reviewer 重覆核准。

## Signing Architecture

主 application 只產生 intent、配置 nonce、呼叫 signer gateway 並 broadcast；不持有 private key。Signer 對 hash/idempotency、chain、asset、token、source、amount、expiry 與 replay 自行驗證。

## Physical Signer Architecture

`server/signer` 使用獨立 H2 durable record，僅允許 dev/test/sepolia 的 encrypted keystore。Signer 使用 immutable atomic amount 對 ETH/USDT 執行 single/daily signing limit，並自行驗證 source wallet 與 ETH/USDT transaction semantics；production/prod 和 Mainnet chain ID 1 會拒絕啟動 local signer。

## Sepolia Implementation

RPC transport 可安全配置 chain ID 11155111。ETH 使用 EIP-1559；USDT 僅接受 configured Sepolia test token contract；builder 與 signer 兩個邊界皆拒絕任意 token contract，並編碼 `transfer(address,uint256)` 的六位小數 amount。

## Mainnet Safety Controls

Mainnet local signing 不可啟動。主 application 不含 raw private key、mnemonic、seed phrase 或 `Credentials.create` path。production signer provider disabled，未有安全 provider 時 fail closed。

## ETH Flow

build unsigned EIP-1559 draft -> signer -> persisted signed raw tx/hash -> RPC broadcast -> receipt/canonical/finalized check -> settlement。

## USDT Flow

同 ETH flow，但 transaction value 為零，to 為 configured ERC-20 contract，data 是 canonical transfer calldata。receipt status 才是成功依據。

## Nonce Strategy

`withdrawal_nonce_states` 以 DB row lock 序列化同一 source/chain，使用 provider pending nonce 與持久 nonce 的最大值，避免 concurrent pod 重用舊 nonce。

## Broadcast Retry Strategy

broadcast timeout、connection reset 或 response lost 都不會重簽。V003 先持久化 txHash、raw transaction 對應 evidence 與 nonce state，再進入 `BROADCAST_UNKNOWN`；worker 只可查鏈或重送相同 raw transaction，principal hold 不得釋放。

## Chain Reconciliation

`BROADCAST_UNKNOWN` 無 receipt 時維持 unknown 交由廣播 worker 對帳／重送，避免提前離開廣播集合；已廣播交易的 receipt 未出現為 CONFIRMING。failed receipt 會保存 gasUsed/effectiveGasPrice，才以 idempotent release 釋放 principal hold 並標為 FAILED；gas 的帳本歸屬仍未實作。只有 canonical block 且 finalized checkpoint 已越過 receipt block 才結算。

## Ledger Settlement

使用 `ReservationCaptureService` 與 `TransactionalLedgerPostingService`，由使用者 SPOT account debit、withdrawal custody account credit，journal business reference 為 WITHDRAWAL。

## Concurrency Controls

idempotency unique key、withdrawal/status compare-and-set、`FOR UPDATE`、`SKIP LOCKED` workers、reservation transaction lock、review uniqueness 與 nonce state lock 防止雙重處理。

## Idempotency Controls

create 接受 body `idempotencyKey` 或既有 header，但同時存在且不同即 conflict；相同 user/payload fingerprint 才 replay。release、capture、ledger post、signing request 及 signer result 都有 durable idempotency evidence。

## Security Controls

unknown risk/velocity/approval/finality 狀態不會 ALLOW。提款 session 僅在 filter 內解析，controller 只接收其 UUID reference，不會記錄或回傳 secret。RPC URL/secret 不會由 application log 輸出；evidence 不保存 session secret 或 key material。

## Runtime Status Matrix

| 項目 | 狀態 | 實作／驗證 |
| --- | --- | --- |
| Approval runtime / RBAC | PARTIAL | `WithdrawalReviewService` 使用 server-side `SuperAdminAccessService`；尚未直接接入 P25 `WithdrawalApprovalPolicy` 的 role evidence model。 |
| Final risk | IMPLEMENTED | `FinalWithdrawalRiskService.advance`。 |
| ETH / USDT builder | IMPLEMENTED | `EthereumTransactionBuilder`、`EthereumTransactionBuilderTest`。 |
| Nonce manager | IMPLEMENTED | `EthereumNonceManager.allocate` 以 Postgres row lock 分配；20-concurrent 專屬測試尚未新增。 |
| Signer gateway / signer idempotency | IMPLEMENTED | `HttpSignerGateway`、`signer/SignerService`、`SignerServiceTest`。 |
| Mainnet signing | DISABLED | `SignerStartupGuard` / disabled production provider。 |
| Broadcast / unknown reconciliation | IMPLEMENTED | `WithdrawalBroadcastService.broadcast`、V003 `BROADCAST_UNKNOWN`。 |
| Receipt / finality | IMPLEMENTED | `WithdrawalChainReconciliationService.reconcile`。 |
| Reservation / ledger settlement | IMPLEMENTED | `ReservationCaptureService`、`TransactionalLedgerPostingService`。 |
| Live Sepolia ETH E2E | NOT RUN | 無受控 RPC、測試 keystore 與 funded wallet。 |
| Live Sepolia USDT E2E | NOT RUN | 無受控 Sepolia token / funded wallet。 |

## Known Limitations

- Mainnet execution、HSM/PKCS#11、mTLS production signer、外部 custody provider 尚未接入，並刻意保持 disabled。
- 目前新增的 runtime tests 覆蓋 state machine、transaction building、startup guard 與 signer idempotency/daily limit；20-concurrent nonce、完整 crash recovery 與 end-to-end database worker 測試仍缺。
- 使用者指定的 `docs/wallet-architecture-assessment.md` 在 repository 不存在，無法作為 runtime assessment input。

## Remaining TODO

- 在具有正式 mTLS 與 HSM/MPC provider 的受控 phase 中完成 Mainnet custody integration。
- 將 P25 `WithdrawalApprovalPolicy` 的 immutable role evidence 實際映射到 server-side RBAC，並完成其 runtime/integration test。
- 加入鎖定資料庫的 nonce concurrency、broadcast ambiguity、receipt failure release 與 settlement crash-recovery 測試。

## Test Results

- `server`: `./mvnw -q test` — 452 tests，0 failures，0 errors，2 skipped；Flyway 已驗證 V001 至 V003。
- `server/signer`: `mvn -q test` — 3 tests，0 failures，0 errors。
- Live Sepolia ETH / USDT E2E：`NOT RUN`；CI 未使用 public RPC。
- 補齊受控整合環境中的 concurrency、broadcast ambiguity、receipt/finality E2E tests 與 metrics dashboard evidence。

## Test Results

最近一次完整 server Maven suite：452 tests，0 failures，0 errors，2 skipped。獨立 signer Maven suite：3 tests，0 failures，0 errors。Sepolia manual E2E：NOT RUN。
