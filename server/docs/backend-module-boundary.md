# Backend Module Boundary

## 目的

本文件定義 LumiX 後端目前的 package 邊界與後續分層方向。
Phase 13 的目標不是把交易 runtime 做出來，而是先把 API 與 bounded context 的邊界釘死，避免後續把帳本、錢包、撮合、結算與外部 API 混在同一層。

## 現況說明

目前 `server/src/main/java/com/lumix` 仍有扁平 package 與 legacy service 類別。
這次只做 boundary marker 與文件，不搬移大量既有程式碼，也不改變 runtime 行為。

## 建議 package 版圖

```text
com.lumix
├─ common
├─ security
├─ user
├─ account
├─ asset
├─ market
├─ wallet
├─ ledger
├─ reservation
├─ order
├─ trade
├─ outbox
├─ audit
└─ admin
```

## 建議 layer

```text
<domain>.api
<domain>.application
<domain>.domain
<domain>.persistence
```

## Layer rule

- `api` 只負責輸入輸出、驗證與對外契約，不能直接塞 domain 規則。
- `application` 只協調 use case 與 transaction boundary，不放複雜資料規則。
- `domain` 放核心規則、值物件與不變式，不依賴 web、db 或 framework 特定細節。
- `persistence` 只處理資料存取與 mapping，不反向滲透 business rule。
- `common` 只放跨 module 的純值型別、錯誤模型與最小共用工具，避免變成依賴中心。

## Transitional packages

下列 package 目前仍是既有程式碼的暫存面，不在本次 runtime 重構範圍內：

```text
com.lumix.spot
com.lumix.openapi
com.lumix.idempotency
```

它們會在後續 phase 依照實際責任拆分或收斂到上述 bounded context。

## Dependency direction

```text
api -> application -> domain
application -> domain
persistence -> domain
common -> all modules
```

禁止方向：

- `domain` 反向依賴 `api` 或 `persistence`
- `application` 直接承接資料庫查詢細節
- `persistence` 夾帶交易規則
- 任何 module 以跨 package import 方式繞過 boundary

## 文字圖

```text
+--------------------+      +------------------+      +------------------+
| api                | ---> | application      | ---> | domain           |
+--------------------+      +------------------+      +------------------+
          |                           |                          ^
          |                           v                          |
          |                    +------------------+              |
          +-------------------> | persistence      | -------------+
                               +------------------+
```

## Phase 13 交付原則

- 先建立 boundary marker，再逐步搬移具體責任。
- 不用 placeholder 假裝 module 已經完成。
- 不碰 ledger、wallet、matching、settlement 的 runtime 實作。

## Error boundary

錯誤回應與 exception handling 的固定 contract 另見：

```text
server/docs/error-response-boundary.md
```

## DTO boundary

API request / response DTO 的命名與驗證規則另見：

```text
server/docs/dto-conventions.md
server/docs/validation-conventions.md
```

## Persistence boundary

repository / persistence 存取規則另見：

```text
server/docs/persistence-boundary.md
```
