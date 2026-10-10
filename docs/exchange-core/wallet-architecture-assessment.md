# Wallet Architecture Assessment

評估日期：2026-10-11。範圍為目前 repository 的 application source、baseline migration、設定與本機部署檔；未執行鏈上、提款或 migration。除非另註明，`EXISTS` 代表可在 application runtime 找到接線，而非只存在 schema、interface 或 pure policy。

## Executive Summary

### Current Situation

LumiX 是單一 Spring Boot API（Maven、Java 21、Spring Boot 3.3.2）加 PostgreSQL、Redis 與本機 Kafka dependency 的 monorepo。錢包領域同時存在早期 `com.lumix.wallet` stub、P22--P25 的 immutable/pure domain contracts，以及部分已實作的 ledger、balance projection、reservation runtime；三者尚未形成可執行的 deposit 或 withdrawal business flow。

唯一可建立的外部鏈連線是受 `lumix.ethereum.observation-enabled` 控制的 Web3j 唯讀 RPC client。它只能讀 `finalized` block 或指定高度 block，不含 transaction、log 掃描、簽章或廣播。預設關閉。提款沒有 public API、Controller、repository、withdrawal persistence writer、approval workflow、signer adapter 或 broadcaster。

### Withdrawal Current Flow

沒有實際 HTTP withdrawal flow。最接近的 legacy path 是 `DefaultWithdrawService.submitWithdrawal`：驗證輸入後回傳一個 in-memory `WithdrawRecord`（固定 `SUBMITTED`），不寫 DB、不 freeze balance、不調 ledger、不簽章、不廣播（`src/main/java/com/lumix/wallet/DefaultWithdrawService.java:25-51`）。該類別沒有 Spring stereotype，且搜尋不到使用點。

P24/P25 的 `WithdrawalRequestAdmissionPolicy`、transition、approval、signing intent 與 broadcast reconciliation 都是 caller-supplied data 的 pure policies；沒有 Controller、DB persistence 或 adapter invocation。提款目前停止於資料契約／預設拒絕 custody gate，而非 signer。

### Existing Security Controls

已有 server-side session cookie authentication、successful-login IP/device snapshot、trusted-device/new-device email verification（設定預設 disabled）、自動的新裝置資金移轉限制、super-admin restricted user withdrawal freeze、PostgreSQL `FOR UPDATE` reservation locking、idempotency table、ledger/outbox/audit append。這些能力均未被 withdrawal request 接線使用。

`WithdrawalExecutionService` 以 `lumix.withdrawal.enabled=false` 為預設，且唯一 `CustodyProvider` implementation 是 `DisabledCustodyProvider`，任何呼叫都回 `REJECTED_DISABLED`（`src/main/java/com/lumix/withdrawal/custody/WithdrawalExecutionService.java:22-25`；`DisabledCustodyProvider.java:7-12`）。

### Important Missing Components

缺少可執行 withdrawal API/orchestrator、withdrawal-to-reservation bridge、request persistence/state store、risk and velocity runtime、address book runtime、transaction-specific MFA、real manual-review workflow、maker-checker enforcement、custody/HSM/MPC adapter、signing、broadcast、chain-confirmation worker、deposit scanner orchestration、checkpoint/event persistence runtime、deposit credit runtime、outbox dispatcher 及 Kafka consumer/producer。

### Biggest Risks

**CRITICAL (if a real withdrawal endpoint were introduced by directly reusing legacy stub):** `DefaultWithdrawService` does not persist, reserve, risk-check, approve, sign, or broadcast; it returns `SUBMITTED` in memory. It must not be exposed as a money-movement service.

**HIGH:** withdrawal schema includes status and request-id constraints, but no application state machine or persistence layer enforces it. The `withdrawal_frozen_at` restriction exists, but there is no withdrawal runtime that consumes it.

**HIGH:** there is no executable address-whitelist/cooldown, withdrawal velocity, or transaction-level 2FA control. The record type `WithdrawAddress` is not persistence/API backed.

No application-side raw private-key handling was found in the specified source/configuration search. No secret value is recorded in this document.

### Recommended Next Step

Before enabling any withdrawal, build an orchestrator that persists an idempotent request and atomically invokes a dedicated `WITHDRAWAL_HOLD` reservation after authentication, restriction and risk evidence checks. Only after that boundary is proven should review, final risk, isolated custody/signer and broadcast/confirmation adapters be added. Reuse the existing PostgreSQL transaction/idempotency/audit/ledger infrastructure; do not expose the legacy wallet stub.

## Repository Baseline

| Item | Evidence / value |
|---|---|
| Repository name | `lumix-server` (`pom.xml:12-15`) |
| Current branch | `main` |
| Current commit | `8bd29eac9bae8ca9641caa35e6e22ba06085fa17` |
| Build system | Maven (`pom.xml`) |
| Java version | 21 (`pom.xml:17-20`) |
| Spring Boot version | 3.3.2 (`pom.xml:6-10`) |
| Database | PostgreSQL (`pom.xml:51-54`, `application-infrastructure.yml:7-12`) |
| Migration framework | Flyway (`pom.xml:42-49`) |
| Cache | Redis / Spring Data Redis; actual cache use: NOT FOUND (`pom.xml:31-34`) |
| Message queue | Kafka exists only in local Compose; application producer/consumer: NOT FOUND (`../docker-compose.yml:48-72`) |
| Scheduler | NOT FOUND (`@Scheduled`, Quartz and ShedLock searches returned no application usage) |
| Deployment model | API jar Dockerfile plus host-run API/web/admin process convention; Compose runs PostgreSQL, Redis and Kafka only (`../devops/server/Dockerfile`, `../docker-compose.yml`, `../README.md:26`) |
| Kubernetes / Helm / replicas / autoscaling | UNKNOWN; no matching files found under available deployment paths |

## Relevant Modules

| Module/package | Purpose and important classes | Runtime assessment |
|---|---|---|
| `com.lumix.wallet` | Legacy wallet models and interfaces: `DefaultWithdrawService`, `DefaultDepositService`, `WalletService`, `WalletGateway`, `WithdrawRecord`. | Stub/contracts; no Spring wiring for default services. |
| `com.lumix.withdrawal.request` | Immutable request, destination/network format, admission/idempotency and lifecycle policies. | Pure contract only. |
| `com.lumix.withdrawal.approval` | Role separation/limit/expiry policy: `WithdrawalApprovalPolicy`. | Pure policy; no identity/RBAC lookup or persistence. |
| `com.lumix.withdrawal.signing` | SHA-256 intent binding and capability dispatch envelope. | Pure policy; no transaction serialization or signer call. |
| `com.lumix.withdrawal.broadcast` | Evidence reconciliation statuses/validation. | Pure read-only policy; no RPC/broadcast. |
| `com.lumix.withdrawal.custody` | `CustodyProvider`, `WithdrawalExecutionService`, disabled implementation. | Spring fail-closed gate; no enabled provider. |
| `com.lumix.deposit.address`, `.observation`, `.credit` | Address ownership, finality/reorg, credit idempotency/handoff/reconciliation contracts. | Pure foundations; no provider/persistence/credit runtime. |
| `com.lumix.ethereum` | Web3j readonly RPC, ETH/USDT candidate recognizers, finalized-block policy, whitelist. | RPC adapter can instantiate only when explicitly enabled; scanners are un-wired helpers. |
| `com.lumix.ledger` / `.runtime` | Double-entry journal/entry append, idempotency, outbox/audit evidence and balance projection refresh. | JDBC transactional runtime exists. |
| `com.lumix.reservation` | PostgreSQL hold/release/capture services with locks and idempotency. | Runtime exists, but hold currently inserts `ADJUSTMENT`/`ADMIN_HOLD`, not withdrawal. |
| `com.lumix.account.projection` / `.history` | Authenticated balance/account/ledger-history read APIs. | Runtime query APIs. |
| `com.lumix.user.auth` | Session, password, login history, device and login-verification controls. | Runtime exists; not withdrawal-specific. |
| `com.lumix.admin.user` | Super-admin login/withdrawal restriction. | Runtime exists and writes audit evidence. |
| `com.lumix.risk.control` / `admin.control` / `audit.evidence` | Policy/evidence models for risk, dual control and audit. | Pure contracts, not withdrawal wiring. |
| `com.lumix.infrastructure.redis` | Standalone/cluster Redis connection and `RedisTemplate`. | Reusable infrastructure; no lock/rate-limit component. |

## Current Architecture

```text
Authenticated user -> BalanceProjectionQueryController -> BalanceProjectionQueryService
                   -> JdbcBalanceProjectionQueryRepository -> balance_projections

Super admin -> AdminUserRestrictionController -> AdminUserRestrictionService
            -> users.withdrawal_frozen_at / audit_logs

Authorized ledger caller -> TransactionalLedgerPostingService -> ledger_journals / ledger_entries
                                                            -> LedgerBalanceProjectionUpdater
                                                            -> balance_projections / outbox_events / audit_logs

Reservation caller -> Hold / Release / Capture services
                   -> balance_projections / reservations / idempotency_keys / audit_logs

Web3jEthereumRpcClient (infrastructure profile and explicitly enabled)
                   -> external Ethereum JSON-RPC -> finalized or block-by-number read only

Withdrawal Controller -> NOT FOUND; no executable withdrawal flow
```

The architecture has no application Kafka client, queue listener, scheduler, external wallet provider or Ethereum persistence coordinator. `ChainProviderRegistry` is Spring-managed, but its only provider is `MockChainProvider` under `chain-mock` profile (`src/main/java/com/lumix/wallet/provider/ChainProviderRegistry.java:20-36`; `provider/mock/MockChainProvider.java:20-37`).

## Withdrawal Current Flow

### Actual executable path

No request mapping containing `withdraw`/`withdrawal` exists. Therefore `POST /withdraw`, `POST /api/withdrawals`, `createWithdrawal`, `requestWithdrawal` and `submitWithdrawal` HTTP paths are **NOT FOUND**.

The only method named `submitWithdrawal` is a legacy in-memory method:

```text
Caller (no Controller or Spring bean found)
  -> DefaultWithdrawService.submitWithdrawal(WithdrawRecord)
     src/main/java/com/lumix/wallet/DefaultWithdrawService.java:25
  -> validates positive amount/non-negative fee and accepted input status
     :40-52
  -> returns a new record with txHash=null and status=SUBMITTED
     :35-37
  -> STOP: no balance check/reserve, DB insert, audit, signer or broadcast
```

`WithdrawalExecutionService.execute(CustodyWithdrawalRequest)` is separately injectable, but it is not referenced by a Controller or withdrawal service. It rejects before provider call when disabled; when enabled, the only installed provider still returns `REJECTED_DISABLED` (`src/main/java/com/lumix/withdrawal/custody/WithdrawalExecutionService.java:22-25`).

### P24/P25 contract sequence (not a runtime flow)

`WithdrawalRequestAdmissionPolicy.evaluate` accepts/replays/rejects only against a caller-provided map (`WithdrawalRequestAdmissionPolicy.java:14-32`); it does not query balance or create a hold. `WithdrawalRequestTransitionPolicy.evaluate` transforms immutable state in memory and appends in-memory audit evidence (`request/lifecycle/WithdrawalRequestTransitionPolicy.java:15-39`). Approval, signing and broadcast classes similarly do not persist or dispatch. These are valuable future boundaries, not current processing.

## Withdrawal State Machine

Two separate state vocabularies exist; neither is persisted/controlled by an executable withdrawal workflow.

### Legacy schema/model statuses

| Status | Meaning from code/schema | Who can enter | Next possible status |
|---|---|---|---|
| `SUBMITTED` | Legacy initial stub result | Legacy stub only | UNKNOWN; no transition runtime |
| `RISK_REVIEW`, `ADMIN_REVIEW`, `APPROVED`, `REJECTED`, `BROADCASTING`, `CHAIN_PENDING`, `SUCCESS`, `FAILED`, `CANCELED` | Enum/schema labels | UNKNOWN | UNKNOWN |

Evidence: `src/main/java/com/lumix/wallet/WithdrawStatus.java:6-17`; schema constraint `src/main/resources/db/migration/V001__baseline.sql:1113-1118`.

### P24 immutable request lifecycle

| Status | Meaning | Who can enter | Next possible status |
|---|---|---|---|
| `REQUESTED` | request created | caller of `WithdrawalRequestState.created` | cancel, expire, manual review |
| `ELIGIBILITY_PENDING` | enum value | No transition into this value found | cancel, expire, manual review if an external state is supplied |
| `MANUAL_REVIEW_PENDING` | pure manual-review queue state | transition policy | cancel, expire, approval handoff |
| `CANCELLED` / `EXPIRED` | terminal for corresponding action | transition policy | none |
| `APPROVAL_HANDOFF_READY` | data handoff, not approval | transition policy | none |

```text
REQUESTED
  -> MANUAL_REVIEW_PENDING       QUEUE_MANUAL_REVIEW
  -> CANCELLED                   CANCEL
  -> EXPIRED                     EXPIRE

ELIGIBILITY_PENDING
  -> MANUAL_REVIEW_PENDING       QUEUE_MANUAL_REVIEW
  -> CANCELLED                   CANCEL
  -> EXPIRED                     EXPIRE

MANUAL_REVIEW_PENDING
  -> APPROVAL_HANDOFF_READY      PREPARE_APPROVAL_HANDOFF
  -> CANCELLED                   CANCEL
  -> EXPIRED                     EXPIRE
```

Illegal P24 transitions are rejected using an expected-state argument (`STALE_STATE_REJECTED` / `INVALID_TRANSITION_REJECTED`), so it is a centralized pure state machine. It is **not** a DB optimistic lock, no `setStatus(...)` write was found, and no persisted workflow invokes it. `ELIGIBILITY_PENDING` has no inbound transition in current code.

## Balance / Ledger Architecture

1. Balance snapshot is `balance_projections` (`V001__baseline.sql:297-330`), keyed by account/asset, with `total_amount`, `available_amount`, `locked_amount`, `projection_version`, `projected_at`, `reconciled_at`.
2. Available balance is `balance_projections.available_amount`; locked/frozen monetary amount is `locked_amount`.
3. Immutable double-entry source of truth is `ledger_journals` plus `ledger_entries`; `TransactionalLedgerPostingService` appends journals/entries in one `@Transactional` method and writes idempotency, outbox and audit evidence (`ledger/runtime/TransactionalLedgerPostingService.java:54-74,143-200`).
4. `LedgerBalanceProjectionUpdater` recomputes total from journal entries and retains reservation lock amount; it never accepts a caller-supplied balance (`LedgerBalanceProjectionUpdater.java:15-18,67-89`).
5. Reservation services do modify `available_amount`/`locked_amount` atomically and use `SELECT ... FOR UPDATE`. However `ReservationHoldService` hard-codes `business_reference_type='ADJUSTMENT'` and `reservation_type='ADMIN_HOLD'` (`ReservationHoldService.java:32`), so it is **not a withdrawal hold**.
6. No withdrawal request creates a freeze; no withdrawal failure release path exists. `ReservationReleaseService.release` exists generically, but no withdrawal caller is found.
7. Deposit credit into ledger/balance is NOT IMPLEMENTED. P23 has only handoff/reconciliation policies.
8. `@Transactional` boundaries relevant to assets: ledger posting (`TransactionalLedgerPostingService.java:54`), hold (`ReservationHoldService.java:23`), release (`ReservationReleaseService.java:22`), capture (`ReservationCaptureService.java:18`), administrator asset adjustment and transfer services. There is no withdrawal transaction boundary.

Concurrency: no `@Version`, Redisson, ShedLock, or distributed lock found. PostgreSQL row locking protects reservation rows/projection rows. The direct concurrent-withdrawal answer is **UNKNOWN / NOT APPLICABLE** because no withdrawal entry exists; a future integration must use an atomic request-idempotency claim plus an account/asset lock/hold transaction. Two pods cannot process a current withdrawal because no worker exists.

## Deposit Architecture

Supported Ethereum observation assets are ETH (18 decimals) and official Ethereum-mainnet USDT (6 decimals) in `EthereumAssetRegistry.java:18-45`. The `ethereum_deposit_events` table is also constrained to ETH/USDT and network `ETHEREUM` (`V001__baseline.sql:1920-1958`). Legacy schema additionally permits TRC20/ERC20/BTC/SOL, but active runtime support for those chains is NOT FOUND.

Deposit discovery runtime is **NOT FOUND**: no polling loop, block scanner coordinator, webhook endpoint, scheduled job, checkpoint repository/writer or scanner persistence service. `EthereumEthDepositScanner` and `EthereumUsdtDepositScanner` only map caller-supplied transfers plus an in-memory address map to candidates (`ethereum/scanner/*.java`).

Finality policy exists: `Web3jEthereumRpcClient.finalizedBlock()` uses Web3j `DefaultBlockParameterName.FINALIZED` (`Web3jEthereumRpcClient.java:41-48`), and `EthereumFinalityService` compares the observed/canonical hashes and finalized height (`EthereumFinalityService.java:16-28`). It does **not** use `latest`, `safe`, N confirmations, or a runtime reorg handler. P22 pure policies model confirmations/reorg, but have no I/O integration. Deposit idempotency is schema-level unique `(network, tx_hash, log_index)` for Ethereum observations; ledger credit idempotency is only a pure P23 policy.

## Blockchain / Ethereum Integration

| Question | Finding |
|---|---|
| Library | Web3j core 4.12.3 (`pom.xml:55-61`) |
| RPC abstraction | `EthereumRpcClient` exposes only `finalizedBlock()` and `blockByNumber()` (`ethereum/rpc/EthereumRpcClient.java:12-15`) |
| Implementation | `Web3jEthereumRpcClient`, profile `infrastructure`, only when `lumix.ethereum.observation-enabled=true` (`Web3jEthereumRpcClient.java:25-38`) |
| RPC URL | `LUMIX_ETHEREUM_RPC_URL` environment binding; value intentionally omitted (`application-infrastructure.yml:87-93`) |
| chainId | `LUMIX_ETHEREUM_CHAIN_ID`, default 1; properties describe Mainnet (`application-infrastructure.yml:91`; `EthereumProperties.java:15-18`) |
| Mainnet / Sepolia | Mainnet default/asset registry only. Sepolia support: NOT FOUND. Configurable chain ID alone is not proof of Sepolia support. |
| Timeout | OkHttp connect default 5s and read default 15s, configurable (`EthereumProperties.java:16-18`; `Web3jEthereumRpcClient.java:34-37`) |
| Retry | NOT FOUND |
| Alchemy dependency | No direct dependency; comments name it as a possible provider. Adapter uses generic HTTP JSON-RPC. |
| Broadcast | NOT FOUND; `EthereumRpcClient` intentionally exposes no broadcast/signing method. |

## Private Key / Signing Audit

Searched application/configuration source for `privateKey`, `private_key`, `PRIVATE_KEY`, `mnemonic`, `MNEMONIC`, `seedPhrase`, `Credentials.create`, `ECKeyPair`, `TransactionEncoder`, `signMessage`, `signTransaction`, `RawTransaction`, and `eth_sendRawTransaction`.

**Result: No application-side raw private-key handling found.** `pom.xml:56` explicitly documents that Web3j is for read-only observation and that `Credentials`/private-key signing are not introduced. `Web3jEthereumRpcClient.java:20-23` likewise documents no `Credentials`. No secret value is included here.

## CustodyProvider

`CustodyProvider` exists with exactly this signature:

```java
CustodyWithdrawalResult createWithdrawal(CustodyWithdrawalRequest request)
```

Path: `src/main/java/com/lumix/withdrawal/custody/CustodyProvider.java:4`.

Implementation inventory: only `DisabledCustodyProvider`; no Fireblocks, BitGo, Copper, HSM, MPC or external-signer adapter implementation was found. It is not a mock; it fail-closes with status `REJECTED_DISABLED`. `lumix.withdrawal.enabled` defaults to `false` (`application-infrastructure.yml:94-96`). If false, `WithdrawalExecutionService` throws `WithdrawalExecutionDisabledException`; if true without replacement provider, the disabled provider still rejects. No code links this service to an API or request lifecycle.

## Withdrawal Idempotency

The `withdrawals` table has `uq_withdrawals_request_id UNIQUE (request_id)` (`V001__baseline.sql:1116-1118`), but no withdrawal INSERT runtime uses it. `idempotency_keys` provides unique `(scope, idempotency_key)` and includes `WITHDRAWAL_REQUEST` in allowed scopes (`V001__baseline.sql:1299-1344`), but no HTTP interceptor/service claims it for withdrawals.

P24 admission has in-memory/map idempotency by `(ownerUserId, key)` and rejects a conflicting payload (`WithdrawalRequestAdmissionPolicy.java:25-45`). P25 signing intent has map idempotency by `(requestId, key)` (`WithdrawalSigningIntentPolicy.java:24-45`). Neither survives a process restart nor protects provider retries.

Consequences: HTTP retry/message retry/provider-timeout duplicate signing are **NOT PROTECTED BY AN EXECUTABLE WITHDRAWAL FLOW**. Because no such flow can send a transaction today, “duplicate withdrawal may be possible” is a conditional future risk, not an observed current transfer behavior.

## Existing Risk Controls

### Withdrawal limits

Single-request maximum is a caller-supplied `BigInteger` in P24 and a caller-supplied approval maximum in P25; no configured/runtime per-transaction, daily, hourly, count, velocity or aggregate-withdrawal limit exists. `risk.control` has a pure `RiskPolicy(maximumAtomicAmount)` decision policy, not a persisted policy store or invocation path.

### Device and IP

**EXISTS for login, NOT IMPLEMENTED for withdrawal evaluation.** Sessions persist `ip_address`, `device_id`, `device_label`; trusted devices retain `last_ip_address` and a user-agent digest (`V001__baseline.sql:1592-1608,1667-1687`). `LoginRequestMetadataResolver` derives bounded IP/device metadata. New device verification can apply `fund_transfer_restricted_until` for 24h by default (`application-infrastructure.yml:57`; `JdbcUserAuthenticationRepository.java:210-221`). No withdrawal service reads IP, device, country, VPN/proxy/Tor or this restriction.

Country/geo/VPN/proxy/Tor controls: NOT IMPLEMENTED. Login IP history exists; withdrawal IP history: NOT IMPLEMENTED.

### Account security changes

Password change is implemented and `user_credentials.password_changed_at` is stored (`V001__baseline.sql:1578-1589`; repository update at `JdbcUserAuthenticationRepository.java:456`). 2FA/TOTP change, email change, phone change and account recovery security-event history: NOT FOUND / NOT IMPLEMENTED. Password reset exists but a generalized security-event table/writer for these changes was not found.

### Addresses

`WithdrawAddress` is only a record (`wallet/WithdrawAddress.java:12-39`) and no withdraw-address table, CRUD API, validation workflow, edit history, cooldown or deletion audit was found. The `withdrawals.address` schema column stores a per-withdrawal destination snapshot, not an address book.

## Authentication / 2FA

`ApiAuthenticationFilter` provides session-based authentication for `/api/v1/**` (see `src/main/java/com/lumix/infrastructure/security/ApiAuthenticationFilter.java`). User auth endpoints include login/logout/password change and new-device email verification (`UserAuthenticationController.java:42-180`). There is no JWT issuance path found; access uses HttpOnly server-session cookies as described by the schema comment (`V001__baseline.sql:1645-1654`).

Withdrawal API does not exist, so its exact authentication requirement is **UNKNOWN**. No TOTP, SMS OTP, passkey, transaction password, or withdrawal-time re-authentication/2FA code was found. The admin *pure* policy contains an MFA evidence boolean, but it is not a live admin MFA runtime nor a withdrawal control (`admin/control/AdminAuthorizationPolicy.java:4`).

## Manual Review / Approval

No withdrawal review/approve/reject API, role assignment, persistence, audit writer, or reviewer queue exists. `WithdrawalApprovalPolicy` does enforce distinct reviewer IDs, rejects request-owner approval, requires caller-supplied roles, amount limit and expiry (`withdrawal/approval/WithdrawalApprovalPolicy.java:18-61`), but explicitly does not verify authorization or write a result. It is a **PARTIAL contract**, not actual maker-checker.

No code directly updates `withdrawals.status`; no withdrawal status writer was found. Super-admin users can set a user's `withdrawal_frozen_at`, not approve a withdrawal, and this action is transactional/audited (`AdminUserRestrictionService.java:63-99`).

## Redis, Message Queue and Background Jobs

Redis topology supports standalone or cluster and supplies `RedisTemplate<String,String>` (`infrastructure/redis/RedisTopologyConfiguration.java:18-48`). Current uses are registration email Bloom filter and CAPTCHA services, not session storage, distributed locks, withdrawal counters or rate limiting. Thus Redis is reusable for a future velocity counter, but no counter/rate-limit infrastructure exists.

Kafka is defined in local Compose but application dependency/configuration, producer, consumer, retry, DLQ and ordering configuration are NOT FOUND. `outbox_events` is written during ledger posting, but no dispatcher exists. No `@Scheduled`, Quartz, ShedLock, deposit/withdrawal/confirmation background job exists.

## Database Tables

| Table | Purpose | Important columns | Important constraints / runtime note |
|---|---|---|---|
| `accounts`, `account_assets` | account/asset ownership | account ID/category/status; asset symbol | FK base for balances and wallet records |
| `balance_projections` | query-side balances | total, available, locked, projection version | updated by ledger projection and reservation runtime |
| `ledger_journals`, `ledger_entries` | append-only double-entry ledger | business reference, request ID; account/asset/direction/amount | runtime append exists |
| `reservations` | hold lifecycle | original/remaining/consumed/released amount, business ref | amount-consistency checks; no withdrawal integration |
| `idempotency_keys` | generic idempotency record | scope/key/request/resource/status | unique `(scope,idempotency_key)` |
| `outbox_events` | transactional event evidence | aggregate/event/payload/status | no dispatch runtime |
| `audit_logs` | immutable operational audit record | actor/action/target/request/outcome/state snapshots | runtime writers exist for ledger/reservation/admin actions |
| `deposit_addresses` | deposit address registry | account/asset/chain/address/status | unique chain/address and account/asset/chain; no allocation runtime |
| `chain_transactions` | observed chain transaction | tx hash/network/status/confirmations | no generic chain observer runtime |
| `deposits` | deposit workflow record | account/asset/address/chain tx/confirmations/credited_at/status | unique chain transaction; no writer/credit runtime |
| `withdrawals` | withdrawal workflow record | request/account/asset/chain/destination/amount/fee/status | unique request ID and chain transaction; no writer/state runtime |
| `ethereum_deposit_events` | immutable ETH/USDT observation | tx/log/block/hash/raw amount/status | unique `(network,tx_hash,log_index)`; no writer coordinator |
| `block_scan_checkpoints` | Ethereum scan checkpoint | network/scanner type/last block/hash | PK `(network,scanner_type)`; no runtime writer |
| `user_sessions`, `user_login_devices`, `login_verification_requests` | auth IP/device evidence | IP/device metadata, digests, timestamps | implemented auth runtime |
| `user_credentials` | password credential | BCrypt hash, `password_changed_at` | no plaintext passwords |

Schema sources: `src/main/resources/db/migration/V001__baseline.sql:297-385,786-1128,1180-1348,1482-1533,1578-1752,1920-1984`.

## Deployment and Secret Management

The server Dockerfile runs a non-root `lumix` user. Docker Compose intentionally starts only PostgreSQL, Redis and single-node development Kafka; the README says Spring Boot API is host-run on 8080. Wallet-specific dedicated service, internal-only signer service, production pod count and autoscaling are UNKNOWN / NOT FOUND.

Secrets/configuration use Spring environment placeholders in `application-infrastructure.yml`, including database, Redis, SMTP and `LUMIX_ETHEREUM_RPC_URL`. The RPC variable **exists** at lines 87-93 and is configured through an environment variable; its value is not reported. Kubernetes Secret, Vault, AWS Secrets Manager, `.env` operational usage and production secret injection are UNKNOWN / NOT FOUND in inspected files.

## API Inventory

| Method | Path | Controller | Purpose | Authentication |
|---|---|---|---|---|
| GET | `/api/v1/assets/balances` | `BalanceProjectionQueryController` | user balance projection read | session filter / authenticated request attribute |
| GET | `/api/v1/assets/history` | `AssetLedgerHistoryQueryController` | user ledger history read | session filter |
| GET | `/api/v1/assets/accounts` | `AccountInventoryQueryController` | account inventory read | session filter; infrastructure profile |
| POST | `/api/v1/assets/transfers` | `InternalTransferController` | internal user transfer | session filter |
| POST | `/api/v1/auth/login` | `UserAuthenticationController` | session login | public login boundary |
| GET | `/api/v1/account/login-history` | `UserLoginHistoryController` | self login IP/device history | session filter |
| GET/DELETE | `/api/v1/account/security`, `/devices/{deviceId}` | `UserLoginSecurityController` | device list/revoke | session filter |
| PUT | `/api/admin/v1/users/{userId}/restrictions/withdrawal` | `AdminUserRestrictionController` | set withdrawal freeze | authenticated super-admin service check |
| ANY | withdrawal create/detail/approve/reject/broadcast | NOT FOUND | NOT IMPLEMENTED | N/A |

## Current Withdrawal Trust Boundary

```text
User -> Withdrawal API (NOT FOUND) -> Withdrawal orchestrator (NOT FOUND)
     -> withdrawals / reservations bridge (NOT FOUND)
     -> review approval runtime (NOT FOUND)
     -> signer gateway (NOT FOUND)
     -> DisabledCustodyProvider only
     -> blockchain broadcast (NOT FOUND)

Super admin -> AdminUserRestrictionService -> users.withdrawal_frozen_at / audit_logs
```

Current ability to change amount/destination: any caller that constructs the legacy `WithdrawRecord` or P24 immutable input can supply them, but no production API persists or executes it. Approval: no component can approve an actual withdrawal. Future signer: only a disabled custody abstraction can receive a fixed signing intent; it cannot sign. The DB has a destination/amount schema snapshot but no writer.

## Confirmed Security Findings

| Severity | Finding | Evidence |
|---|---|---|
| CRITICAL (conditional) | Legacy withdrawal stub must never be exposed: no persistence, hold, risk, audit, signing or broadcast. | `wallet/DefaultWithdrawService.java:16-37` |
| HIGH | Withdrawal lifecycle schema/enum has no executable controller, repository, state transition enforcement or freeze consumption. | `wallet/WithdrawStatus.java:6-17`; no withdrawal runtime search result |
| HIGH | No transaction-level MFA/2FA, address whitelist/cooldown, velocity or risk evaluation is wired to withdrawal. | API/service searches; only pure policies/record exist |
| HIGH | No signer/broadcast implementation; this is a safe availability blocker, not a key-exposure finding. | `withdrawal/custody/DisabledCustodyProvider.java:7-12`; `ethereum/rpc/EthereumRpcClient.java:12-15` |
| MEDIUM | Withdrawal idempotency exists only in schema and in-memory policies, not an executable request-to-provider sequence. | `V001__baseline.sql:1116-1118,1336-1338`; P24/P25 policies |
| MEDIUM | New-device fund-transfer restriction and admin withdrawal freeze are not consumed by a withdrawal path. | `JdbcUserAuthenticationRepository.java:210-221`; `AdminUserRestrictionService.java:63-76` |

No evidence supports a claim that the application stores a production raw private key, broadcasts an Ethereum transaction, or allows an admin to update withdrawal status directly.

## Reusable Existing Components

- PostgreSQL `@Transactional`, `FOR UPDATE`, generic `idempotency_keys`, `audit_logs`, outbox evidence and immutable ledger append: use for an orchestrator/hold/decision boundary.
- `LedgerBalanceProjectionUpdater` and balance query APIs: reuse for post-ledger balance reads, not as a direct mutable balance API.
- Reservation hold/release/capture services: reusable locking/idempotency patterns, but require explicit extension to `WITHDRAWAL_HOLD`; current hard-coded admin hold cannot be reused unchanged.
- Session authentication, successful-login IP/device snapshots, trusted-device/new-device restriction and authenticated account-security endpoints.
- `AdminUserRestrictionService`: reusable source of withdrawal freeze evidence after a withdrawal path explicitly consumes it.
- `WithdrawalRequestAdmissionPolicy`, `WithdrawalRequestTransitionPolicy`, `WithdrawalApprovalPolicy`, signing intent and custody interfaces: reusable domain boundaries only; persistence/authz/integration must be added.
- `EthereumRpcClient` and finality policy: reusable read-only provider boundary for an authorized deposit observation worker.
- Redis `RedisTemplate`: reusable infrastructure, not an existing rate-limit/lock facility.

## Missing Capabilities

| Capability | Status | Evidence |
|---|---|---|
| Withdrawal orchestrator/API/persistence | MISSING | no Controller/repository/service path |
| Withdrawal balance freeze | MISSING | reservation runtime exists but no withdrawal bridge |
| Deposit scanner coordinator/checkpoint writer | MISSING | only RPC/helper scanners/schema |
| Deposit credit/reversal runtime | MISSING | P23 pure handoff only |
| Executable withdrawal state machine | MISSING | pure P24 state policy only |
| Request/provider idempotency persistence | PARTIAL | schema plus in-memory policies |
| Risk engine/velocity/device/IP scoring | MISSING | policy/auth evidence only |
| Manual review and maker-checker runtime | MISSING | pure approval policy only |
| Withdrawal address book/cooldown | MISSING | record only; no table/API |
| Transaction-level MFA | MISSING | login/new-device only |
| Custody adapter / HSM / MPC / local signer | MISSING | disabled provider only |
| Ethereum broadcast / confirmation worker | MISSING | RPC read-only only |
| Chain provider abstraction | PARTIAL | generic contract + chain-mock only |
| Audit infrastructure | EXISTS | table and several runtime writers |

## Gap Matrix

| Capability | Status | Existing Component | Gap |
|---|---|---|---|
| Deposit scanner | PARTIAL | Ethereum candidate helpers, RPC client | no worker/RPC traversal/persistence |
| Deposit finality | PARTIAL | finalized tag and pure finality policy | no event lifecycle integration |
| Deposit idempotency | PARTIAL | Ethereum unique constraint; P23 policy | no deposit-credit writer |
| Ledger | EXISTS | transactional append + projections | not connected to deposits/withdrawals |
| Balance freeze | PARTIAL | reservation runtime | no `WITHDRAWAL_HOLD` bridge |
| Withdrawal state machine | PARTIAL | P24 pure transition policy | no persistence/workflow |
| Withdrawal idempotency | PARTIAL | schema and pure policies | no HTTP/provider runtime |
| Withdrawal review | PARTIAL | P25 policy | no queue/API/persistence |
| Maker/checker | PARTIAL | pure distinct-reviewer policy | no authz/runtime enforcement |
| 2FA | MISSING | new-device email login verification only | no withdrawal 2FA/TOTP |
| Device tracking | EXISTS | session/device tables and services | not evaluated at withdrawal |
| IP tracking | EXISTS | session/login history | not captured/evaluated at withdrawal |
| Address book | MISSING | `WithdrawAddress` record | no table/API/history |
| Address cooldown | MISSING | none | none |
| Velocity limits | MISSING | Redis available | no counters/policy invocation |
| Risk Engine | PARTIAL | pure `risk.control` policy | no data/feed/runtime |
| Audit | EXISTS | `audit_logs`, runtime writers | no withdrawal audit path |
| Custody abstraction | EXISTS | `CustodyProvider` | disabled implementation only |
| Local signing | MISSING | none | no keys/Credentials/signing |
| External signing | MISSING | capability enum/interface | no adapter |
| HSM | MISSING | provider-kind enum | no integration |
| RPC | PARTIAL | read-only Web3j Ethereum RPC | no polling/logs/transactions/retry |
| Broadcast | MISSING | broadcast evidence policy | no provider call |
| Chain confirmation | PARTIAL | finality policy | no worker/status persistence |

## Suggested Next Architecture Changes

### Phase 1 — request and reserve boundary

Add a persisted authenticated withdrawal request API/orchestrator. Atomically: claim request idempotency, read user restriction/new-device evidence, validate destination/network/asset, create a true `WITHDRAWAL_HOLD`, record audit, and persist the expected state. This needs a new bridge/refactor because current reservation hold is explicitly `ADMIN_HOLD`; reuse its transaction/locking/idempotency pattern, not its hard-coded semantics.

### Phase 2 — controlled decision boundary

Persist risk evidence/decisions and review tasks; bind reviewers to actual RBAC and an immutable request snapshot. Add transaction-specific re-authentication and policy-backed limits/address controls. The existing P24/P25 immutable policies can become domain rules, but cannot substitute for persistence, authorization or audit writes.

### Phase 3 — isolated execution and reconciliation

Introduce a separately authorized signer gateway/custody adapter, then provider-specific broadcast and chain-confirmation worker. The gateway must consume the existing intent digest and idempotency semantics but own provider timeout/ambiguous-result reconciliation. No local key/HSM/MPC implementation should be selected before a provider/security task authorizes it. Deposit observer/credit must remain a separate ordered track.

## Architecture Questions

1. **Withdrawal API 的入口在哪？ — NO**  No withdrawal request mapping/controller was found.
2. **Withdrawal 核心 service 是哪個？ — PARTIAL**  Legacy `DefaultWithdrawService` is a stub, not a wired core service; `WithdrawalExecutionService` is a disconnected fail-closed custody gate.
3. **提款時目前有沒有 freeze balance？ — NO**  No withdrawal flow calls reservation hold. Existing hold is `ADMIN_HOLD`.
4. **提款失敗後 balance 如何 release？ — NO**  Generic release exists, but no withdrawal failure integration exists.
5. **是否有 withdrawal state machine？ — PARTIAL**  A pure P24 immutable transition policy exists; legacy DB statuses have no enforced runtime transitions.
6. **是否有 withdrawal idempotency？ — PARTIAL**  Schema and in-memory policy exist; no executable HTTP-to-provider guarantee.
7. **是否有人工審核？ — PARTIAL**  Pure approval policy only; no real queue/API/persistence.
8. **是否有 maker-checker？ — PARTIAL**  Pure policy rejects owner/duplicate reviewers; no live RBAC evidence binding.
9. **是否有 withdrawal address book？ — NO**  Only a Java record, no persistence/API.
10. **是否有新地址 cooldown？ — NO**  Not found.
11. **是否追蹤登入 IP？ — YES**  Sessions/login history/trusted devices retain IP snapshot.
12. **是否追蹤提款 IP？ — NO**  No withdrawal API/event exists to capture it.
13. **是否追蹤 device？ — YES**  Trusted device, device label/platform and security endpoints exist.
14. **是否記錄 password / 2FA / email / phone change？ — PARTIAL**  Password changed time exists. 2FA/email/phone change records are not found.
15. **是否已有 Redis 可以做 velocity counter？ — PARTIAL**  `RedisTemplate` infrastructure exists; no velocity counter implementation.
16. **是否已有 audit log？ — YES**  `audit_logs` and runtime writers for ledger/reservation/admin operations exist; no withdrawal writer.
17. **是否存在 raw private key handling？ — NO**  Specified repository search found none; Web3j path is read-only.
18. **是否已存在 CustodyProvider / Signer abstraction？ — YES**  `CustodyProvider` and signer-capability contracts exist; only disabled provider implementation.
19. **Ethereum transaction 目前由誰 broadcast？ — NO**  Nobody; no broadcast code/provider found.
20. **如果現在打開真實提款，最大的 blocker 是什麼？ — YES (blocker exists)**  There is no end-to-end withdrawal orchestration and custody/broadcast is fail-closed/disabled; therefore it cannot safely move funds.

## File Reference Index

- `pom.xml` — build, Java, Spring Boot, PostgreSQL, Redis, Flyway and Web3j dependencies.
- `src/main/resources/application-infrastructure.yml` — environment configuration, Ethereum observation and withdrawal default-off gates.
- `src/main/resources/db/migration/V001__baseline.sql` — wallet, ledger, balance, reservation, idempotency, audit, auth and Ethereum-observation schema.
- `src/main/java/com/lumix/wallet/DefaultWithdrawService.java` — legacy in-memory withdrawal stub.
- `src/main/java/com/lumix/wallet/DefaultDepositService.java` — legacy in-memory deposit stub.
- `src/main/java/com/lumix/wallet/WalletService.java` — legacy address/asset query contract.
- `src/main/java/com/lumix/wallet/WalletGateway.java` — future provider/broadcast contract, unimplemented.
- `src/main/java/com/lumix/wallet/WithdrawStatus.java` — legacy withdrawal status enum.
- `src/main/java/com/lumix/wallet/WithdrawAddress.java` — non-persisted address record.
- `src/main/java/com/lumix/withdrawal/request/WithdrawalRequestAdmissionPolicy.java` — pure request idempotency/admission.
- `src/main/java/com/lumix/withdrawal/request/lifecycle/WithdrawalRequestTransitionPolicy.java` — pure lifecycle transitions.
- `src/main/java/com/lumix/withdrawal/approval/WithdrawalApprovalPolicy.java` — pure role separation approval.
- `src/main/java/com/lumix/withdrawal/signing/WithdrawalSigningIntentPolicy.java` — intent digest/idempotency contract.
- `src/main/java/com/lumix/withdrawal/signing/WithdrawalSignerDispatchPolicy.java` — future adapter envelope policy.
- `src/main/java/com/lumix/withdrawal/broadcast/WithdrawalBroadcastReconciliationPolicy.java` — read-only broadcast evidence policy.
- `src/main/java/com/lumix/withdrawal/custody/CustodyProvider.java` — custody boundary.
- `src/main/java/com/lumix/withdrawal/custody/DisabledCustodyProvider.java` — only fail-closed custody implementation.
- `src/main/java/com/lumix/withdrawal/custody/WithdrawalExecutionService.java` — default-off execution gate.
- `src/main/java/com/lumix/ledger/runtime/TransactionalLedgerPostingService.java` — atomic ledger/idempotency/outbox/audit runtime.
- `src/main/java/com/lumix/ledger/runtime/LedgerBalanceProjectionUpdater.java` — ledger-derived balance projection updater.
- `src/main/java/com/lumix/reservation/ReservationHoldService.java` — locked/idempotent generic reservation hold implementation.
- `src/main/java/com/lumix/reservation/ReservationReleaseService.java` — reservation release implementation.
- `src/main/java/com/lumix/reservation/ReservationCaptureService.java` — reservation capture implementation.
- `src/main/java/com/lumix/account/projection/BalanceProjectionQueryController.java` — authenticated balance read API.
- `src/main/java/com/lumix/ethereum/config/EthereumProperties.java` — safe Ethereum observation properties.
- `src/main/java/com/lumix/ethereum/rpc/EthereumRpcClient.java` — read-only RPC abstraction.
- `src/main/java/com/lumix/ethereum/rpc/Web3jEthereumRpcClient.java` — profile/flag-gated Web3j implementation.
- `src/main/java/com/lumix/ethereum/finality/EthereumFinalityService.java` — finalized/hash comparison policy.
- `src/main/java/com/lumix/ethereum/asset/EthereumAssetRegistry.java` — ETH/official-USDT whitelist.
- `src/main/java/com/lumix/ethereum/scanner/EthereumEthDepositScanner.java` — caller-supplied ETH candidate mapper.
- `src/main/java/com/lumix/ethereum/scanner/EthereumUsdtDepositScanner.java` — caller-supplied USDT candidate mapper.
- `src/main/java/com/lumix/wallet/provider/ChainProvider.java` — generic chain observation provider contract.
- `src/main/java/com/lumix/wallet/provider/ChainProviderRegistry.java` — healthy capability selection boundary.
- `src/main/java/com/lumix/wallet/provider/mock/MockChainProvider.java` — `chain-mock` profile-only provider.
- `src/main/java/com/lumix/admin/user/AdminUserRestrictionService.java` — super-admin withdrawal freeze plus audit.
- `src/main/java/com/lumix/infrastructure/security/ApiAuthenticationFilter.java` — session API authentication filter.
- `src/main/java/com/lumix/user/auth/application/UserAuthenticationService.java` — session/new-device authentication runtime.
- `src/main/java/com/lumix/user/auth/persistence/JdbcUserAuthenticationRepository.java` — IP/device/restriction persistence.
- `src/main/java/com/lumix/infrastructure/redis/RedisTopologyConfiguration.java` — reusable Redis connectivity.
- `../docker-compose.yml` — local PostgreSQL/Redis/Kafka dependencies.
- `../devops/server/Dockerfile` — server container runtime.

## Final Completeness Check

- Executive Summary: present.
- Mermaid current-architecture and trust-boundary diagrams: present.
- Withdrawal current flow and state machine: present; explicitly distinguishes stub/pure contracts/runtime.
- DB table overview, risk/device/IP analysis, signing/private-key audit, gap matrix, 20 architecture questions and file reference index: present.
- Secret values: none included. Searches report only configuration mechanisms and code locations.
