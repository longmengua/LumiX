# Transaction Boundary

## 目的

本文件定義 LumiX 後端 application service 的 transaction boundary policy。
這一層只規範 transaction 要放在哪裡、哪些 use case 必須是 read-only、哪些必須是 write transaction，不能代表任何 runtime 資金流程已經完成。

## 核心規則

- Controller / API layer 不應直接開 transaction。
- Transaction boundary 應放在 application service 層。
- Repository 不應自行決定跨 aggregate transaction。
- Read-only query 與 write transaction 要分開。
- 高風險流程要明確標記 `HUMAN_REVIEW_REQUIRED`。

## 高風險流程

```text
ledger posting
reservation hold / release
withdrawal request
order placement
settlement
```

這些流程即使只是 policy 或 annotation，也不能讓人誤以為已經實作 money movement。

## 建議邊界

```text
API/controller -> application service -> repository/port
                         |                    |
                         |                    +--> persistence
                         v
                  transaction boundary
```

## Transaction 類型

```text
READ_ONLY  - 純查詢，不應修改任何狀態
WRITE      - 可能寫入資料，但不代表一定是高風險資金流程
```

## 文字圖

```text
+------------------+     +------------------------+     +------------------+
| API / controller | --> | application service    | --> | repository/port  |
+------------------+     +------------------------+     +------------------+
                             |                 |
                             |                 +--> READ_ONLY query
                             v
                        WRITE transaction
```

## Phase 13 原則

- 先把 policy 固定，再讓後續 service 套用。
- 不在此階段加入實際 transaction manager 設定。
- 不讓 repository 決定交易範圍。
