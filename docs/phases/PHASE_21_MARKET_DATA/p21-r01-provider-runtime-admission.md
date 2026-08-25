# P21-R01 正式行情 Provider Runtime Admission

```text
Task: P21-R01
Status: DOCUMENTED_ADMISSION_TASK_READY
HUMAN_REVIEW_REQUIRED: yes
Scope: 將正式 provider runtime 的選擇、授權、驗證與後續 task 邊界固定為可稽核的 admission 契約。
Not authorized: provider selection, contract acceptance, credential issuance, network connection, secret handling, adapter execution, persistence, public transport or production claim.
```

## 任務目的

P21 foundation 僅處理 provider-neutral 的純資料模型；要形成正式行情服務，必須先有可審計的來源選擇與受控接入決策。本 task 將這項外部依賴從 application code 分離：在供應商、資料授權、環境、憑證責任與風險接受未被人類明確指定前，任何 adapter 都不得建立連線或假定可用。

這不是「選定任一行情供應商」的文件，也不是用 mock 或公開網路呼叫替代正式來源的授權。無法取得來源資料時，P21 health 必須保持非 `HEALTHY`，下游不得將資料宣稱為即時或 authoritative。

## 人類必須提供的外部決策

在 P21-R02 開始前，指定的商業、資安與工程責任人必須留下可追溯的決策紀錄，至少包括：

1. provider 法律實體、產品／資料集、使用市場、資產與 instrument 範圍，以及資料再散布權利。
2. 合約／採購狀態、SLA、rate limit、服務中斷通知、歷史資料與 snapshot/resync 支援範圍。
3. 環境區隔、可用的 endpoint allowlist、TLS／憑證要求、網路出口責任與資料所在地限制。
4. credential 的擁有者、建立／輪替／撤銷程序，以及專用 secret manager 的責任人；private key、API secret 與 access token 不得出現在本 repository、一般設定檔、log 或測試 fixture。
5. provider failure、資料延遲、gap、correction、delisting 與 market halt 時的商業決策與 escalation owner。

缺少任一項時，P21-R02 的狀態必須維持 `NOT_AUTHORIZED`。工程 agent 不得自行選擇免費 API、試用帳號、憑證位置或資料再散布政策。

## P21-R02 的最低入口條件

P21-R02 是唯一可開始第一個 provider adapter 的後續 task，且僅能處理已核可的一家 provider 與一個受控環境。開始前必須同時具備：

- 上述五項人類決策的 evidence reference 與有效期限。
- provider payload 與 P21 normalized event contract 的欄位、scale、sequence、source timestamp、snapshot/delta 與 correction 語意的逐項對照。
- 獨立的 secret-management task／審查，證明 application 不可讀出、記錄或回傳 secret；未完成時 adapter 必須 fail-closed。
- 對 endpoint、timeout、retry、rate limit、disconnect、stale、gap、resync 與供應商 outage 的測試計畫；重試不得隱藏 gap 或把舊資料標為 `HEALTHY`。
- 明確證明 adapter output 只能進入 P21 唯讀 projection，沒有通往 matching、order、fill、position、balance、ledger、reservation、settlement 或 wallet 的依賴。

P21-R02 不自動授權 persistence、cache、message broker、公開 API/WebSocket 或 customer-facing transport；每一項必須再拆分為後續獨立 task，並重新檢查 P36 的資料、安全與營運 gate。

## 失敗與停止條件

```text
未選定／未授權 provider        -> NOT_AUTHORIZED；不得建立連線
credential 或網路責任不明       -> NOT_AUTHORIZED；不得建立連線
payload 無法滿足 sequence/time  -> DEGRADE_AND_BLOCK；不得宣稱 authoritative
snapshot/delta 無法安全 resync  -> DEGRADE_AND_BLOCK；不得提供可用 order book
來源 outage、stale 或 gap        -> 非 HEALTHY；下游必須看見 health 狀態
任何資金或交易核心連線           -> STOP；不屬於 P21 範圍
```

## 驗證、審核與 rollback

本 task 的驗證是文件查核：所有外部決策、P21-R02 entry condition、禁止範圍及停止條件均被明確列出，且沒有 provider 名稱、endpoint、credential、secret 或可執行連線實作。`HUMAN_REVIEW_REQUIRED` 的重點是資料授權、secret lifecycle、網路邊界、market-data 完整性與 downstream isolation。

本 task 只新增文件；rollback 為 revert 文件 commit。即使完成 P21-R01，也不會解除 P36 的任何 blocker 或 production launch 禁令。
