# 交易所訊息中心 v1 一頁式規格（後端優先）

生成時間：2026-06-16 06:20 CST

## 目的

這是後端可以直接開工的完整規格，前端依此文件同步接 API。文件不含程式碼與 SQL，只含行為契約與架構規範。

---

## 1. 產品目標

- 給一般使用者提供交易所內所有重要訊息的集中入口：交易、入金、出金、帳戶、安全、法遵、公告、活動、系統通知。
- 支援 Web/App 共用。
- 使用者可：查列表、查詳情、已讀、刪除（軟刪除）、封存、釘選、批次已讀、通知偏好。
- 管理員可：發佈系統/活動/公告，指定對象與排程，支援取消排程。
- 內部系統事件可自動發出訊息（訂單、資產、安全、法遵）。
- 必要時即時推播（WS 或 SSE）。

---

## 2. 角色與權限

### 2.1 一般使用者

- 只能存取自己的訊息。
- 可操作：標記已讀、刪除、封存、釘選、偏好設定。
- 站內通知不能關閉。
- 安全與法遵訊息不能被一般使用者關閉站內通知。

### 2.2 後台管理員

- 可建立與管理公告、排程與狀態。
- 不得任意替一般使用者改已讀/封存/刪除/釘選。

### 2.3 內部系統服務

- 呼叫系統事件 API 建訊息。
- 以事件為中心寫入，需具 service-to-service 鑑權。

---

## 3. 訊息分類與重要性

### 分類
- SYSTEM、ANNOUNCEMENT、ORDER、TRADE、DEPOSIT、WITHDRAW、ACCOUNT、SECURITY、PROMOTION、COMPLIANCE

### 重要性
- INFO / SUCCESS / WARNING / CRITICAL

---

## 4. 核心行為規則

1. 每則訊息具有：標題、正文、分類、重要性、建立時間。
2. 每人視角狀態分離：
   - 已讀/未讀
   - 刪除（軟刪除）
   - 封存
   - 釘選
3. 列表以 cursor 分頁（嚴禁 offset）。
4. 重複操作安全冪等：標記已讀、刪除、封存、釘選可重複呼叫，不可報錯。
5. 去重機制：預設以 `eventType + eventId + userId`。
6. 刪除只隱藏，不刪除底層訊息。

---

## 5. 資料模型（契約層）

### 5.1 Message

- messageId
- templateCode
- title / summary / body
- category / severity
- actionUrl / actionLabel
- createdAt / effectiveAt / expireAt
- isScheduled
- sourceEventType / sourceEventId / sourceEventHash
- createdBy（system / admin）
- visibilityRule

### 5.2 UserMessageState

- messageId、userId
- isRead、readAt
- isDeleted（軟刪除）
- isArchived
- isPinned、pinAt
- lastNotifiedAt

### 5.3 Announcement

- announcementId
- title / summary / category / severity
- templateCode / templateVars
- audienceType：ALL / USER_IDS / VIP / HAS_ASSET / CUSTOM_FILTER
- audienceCondition（各類條件）
- sendAt / expireAt
- status：DRAFT / SCHEDULED / PUBLISHED / CANCELLED
- deliveryMode：DIRECT / LAZY
- dedupeKey

### 5.4 NotificationPreference

- per category：inAppEnabled（固定 true）、emailEnabled、smsEnabled、pushEnabled
- lockedChannels（SECURITY、COMPLIANCE 需加鎖）
- updatedBy / updatedAt

---

## 6. 用戶端互動流程

### 6.1 訊息列表

- 進入入口即查 `GET /api/messages`。
- 預設僅顯示未刪除、未封存、最新未讀優先。
- 顯示：標題、摘要、分類、重要性、時間、未讀/已讀、釘選、跳轉。
- 支援：
  - 全部 / 未讀 / 已封存
  - 分類篩選
  - 搜尋（標題+正文）
  - cursor 分頁
  - 已讀標記

### 6.2 訊息詳情

- 點項目進入 `GET /api/messages/{messageId}`。
- 顯示完整正文與相關資料。
- 可執行：已讀、封存、刪除、釘選/取消。

### 6.3 已讀與批次

- 單則已讀：`POST /api/messages/{messageId}/read`
- 批次：`POST /api/messages/read-all`
- 回應只要成功即可，重複呼叫回成功。

### 6.4 未讀數

- 入口顯示總未讀 + 各分類未讀。
- 不含已刪除；預設不含已封存。

### 6.5 即時更新

- 登入後訂閱 WS / SSE。
- 收到 `message.new` 即時更新列表指示與未讀數。
- 斷線後重連要重拉 `GET /api/messages/unread-count`。

---

## 7. API 規格（v1）

回應規範：
- 成功：`{ ok: true, data: ... }`
- 失敗：`{ ok: false, error: { code, message, traceId } }`
- 時間用 UTC ISO8601（含毫秒）。

### 7.1 使用者 API

- `GET /api/messages`
  - query：
    - `cursor`（可選，字串）
    - `limit`（可選，1~100，預設 30）
    - `status`（可選，`UNREAD|ALL`，預設 `ALL`）
    - `archived`（可選，`true|false`，預設 `false`）
    - `search`（可選，關鍵字）
- `category`（可選，僅支援 repeatable，例如 `category=ORDER&category=TRADE`）
    - `pinnedFirst`（可選，`true|false`，預設 `true`）
    - `excludeDeleted`（可選，`true|false`，預設 `true`）
  - 排序：`createdAt desc, messageId desc`
- `GET /api/messages/{messageId}`
- `POST /api/messages/{messageId}/read`
- `POST /api/messages/read-all`
  - body：`{ scope: "ALL" | "CATEGORY", category?: MessageCategory }`
- `DELETE /api/messages/{messageId}`（軟刪除）
- `POST /api/messages/{messageId}/archive`
- `POST /api/messages/{messageId}/unarchive`
- `POST /api/messages/{messageId}/pin`
- `DELETE /api/messages/{messageId}/pin`
- `GET /api/messages/unread-count`
- `GET /api/message-preferences`
- `PUT /api/message-preferences`

#### 前端可直接串接的參數定義（request / response）

1) `GET /api/messages`

- Query: `cursor, limit, status, archived, search, category, pinnedFirst, excludeDeleted`
- 200 回傳:
  - `data.nextCursor`: `string | null`
  - `data.hasMore`: `boolean`
  - `data.items`: 陣列，每筆：
    - `messageId`: `string`（UUID）
    - `title`: `string`
    - `summary`: `string`
    - `category`: `SYSTEM|ANNOUNCEMENT|ORDER|TRADE|DEPOSIT|WITHDRAW|ACCOUNT|SECURITY|PROMOTION|COMPLIANCE`
    - `severity`: `INFO|SUCCESS|WARNING|CRITICAL`
    - `createdAt`: `ISO8601`
    - `isRead`: `boolean`
    - `isPinned`: `boolean`
    - `isDeleted`: `boolean`
    - `isArchived`: `boolean`
    - `isExpired`: `boolean`
    - `isScheduled`: `boolean`
    - `actionUrl`: `string | null`
    - `actionLabel`: `string | null`

2) `GET /api/messages/{messageId}`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `title`, `summary`, `body`
  - `category`, `severity`
  - `createdAt`, `effectiveAt`, `expireAt`
  - `isRead`, `readAt`
  - `isDeleted`, `isArchived`, `isPinned`
  - `isExpired`, `isScheduled`
  - `actionUrl`, `actionLabel`
  - `metadata`: `object`

3) `POST /api/messages/{messageId}/read`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `isRead`: `true`
  - `readAt`: `ISO8601`

4) `POST /api/messages/read-all`

- Request body:
  - `scope`: `ALL` 或 `CATEGORY`（必填）
  - `category`: `MessageCategory`（`scope=CATEGORY` 時必填）
- 200 回傳:
  - `updatedCount`: `number`

5) `DELETE /api/messages/{messageId}`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `deleted`: `true`

6) `POST /api/messages/{messageId}/archive`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `isArchived`: `true`

7) `POST /api/messages/{messageId}/unarchive`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `isArchived`: `false`

8) `POST /api/messages/{messageId}/pin`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `isPinned`: `true`

9) `DELETE /api/messages/{messageId}/pin`

- Path:
  - `messageId`（必填）
- 200 回傳:
  - `messageId`
  - `isPinned`: `false`

10) `GET /api/messages/unread-count`

- Query:
  - `excludeArchived`（可選，預設 true）
- 200 回傳:
  - `unreadCount`: `number`
  - `byCategory`: `Record<MessageCategory, number>`

11) `GET /api/message-preferences`

- 200 回傳:
  - `preferences`: 陣列
    - `category`
    - `inAppEnabled`（固定 true）
    - `emailEnabled`
    - `smsEnabled`
    - `pushEnabled`
    - `locked`（security/compliance 時為 true）

12) `PUT /api/message-preferences`

- Request:
  - `preferences`: 陣列（每筆如上）
- 200 回傳:
  - `updated`: `number`（被更新的分類筆數）
  - `preferences`: 更新後完整結果

### 7.2 管理員 API

- `POST /api/admin/messages/announcements`
  - title, summary, category, severity, templateCode, templateVars, actionUrl, audience, sendAt, expireAt, deliveryMode, dedupeKey
- `POST /api/admin/messages/announcements/{announcementId}/cancel`
- `GET /api/admin/messages/announcements`
- `GET /api/admin/messages/announcements/{announcementId}`

#### 管理員 API 參數（request / response）

1) `POST /api/admin/messages/announcements`
- Request:
  - `title`: `string`
  - `summary`: `string`
  - `category`: `ANNOUNCEMENT|PROMOTION|SYSTEM`
  - `severity`: `INFO|SUCCESS|WARNING|CRITICAL`
  - `templateCode`: `string`
  - `templateVars`: `object`
  - `actionUrl`: `string | null`
  - `audience`:
    - `type`: `ALL|USER_IDS|VIP|HAS_ASSET|CUSTOM_FILTER`
    - `userIds`: `string[]`（type=USER_IDS）
    - `vipLevels`: `number[]`（type=VIP）
    - `assetSymbol`: `string`（type=HAS_ASSET）
    - `customFilter`: `string`（JSON string，可選）
  - `sendAt`: `ISO8601 | null`
  - `expireAt`: `ISO8601 | null`
  - `deliveryMode`: `DIRECT|LAZY`
  - `dedupeKey`: `string | null`
- Response:
  - `announcementId`
  - `status`: `PUBLISHED|SCHEDULED`
  - `estimatedRecipients`: `number`

2) `POST /api/admin/messages/announcements/{announcementId}/cancel`
- Path: `announcementId`
- Response:
  - `announcementId`
  - `status`: `CANCELLED`

3) `GET /api/admin/messages/announcements`
- Query:
  - `status`（可選）
  - `category`（可選）
  - `from`（可選，ISO8601）
  - `to`（可選，ISO8601）
  - `cursor`（可選）
  - `limit`（可選）
- Response:
  - `items`: list（announcement summary）
  - `nextCursor`
  - `hasMore`

4) `GET /api/admin/messages/announcements/{announcementId}`
- Path: `announcementId`
- Response:
  - announcement detail + `deliveryStats`
    - `sent`, `failed`, `skipped`, `pending`

### 7.3 系統事件 API

- `POST /api/system/messages/event`
- `POST /api/system/messages/event/batch`
- `POST /api/system/messages/send`

#### 系統事件 API 參數（request / response）

1) `POST /api/system/messages/event`
- Request:
  - `eventType`: `string`
  - `eventId`: `string`
  - `eventTimestamp`: `ISO8601`
  - `sourceUserId`: `number`
  - `dedupeKey`: `string`
  - `templateCode`: `string`
  - `templateVars`: `object`
  - `category`: `MessageCategory`
  - `severity`: `Severity`
  - `actionUrl`: `string | null`
  - `metadataOverrides`: `object | null`
- Response:
  - `messageId`
  - `userId`
  - `status`: `created|skipped|duplicate`

2) `POST /api/system/messages/event/batch`
- Request:
  - `events`: array（每筆同 `event` 欄位）
- Response:
  - `results`: array
    - `messageId`（若有）
    - `userId`
    - `status`: `created|skipped|duplicate`

3) `POST /api/system/messages/send`
- Request:
  - `uids`: `string[]`
  - `templateCode`: `string`
  - `templateVars`: `object`
  - `category`: `MessageCategory`
  - `severity`: `Severity`
  - `actionUrl`: `string | null`
  - `dedupeKey`: `string | null`
- Response:
  - `acceptedCount`: `number`
  - `dedupeSkippedCount`: `number`
  - `status`: `string`

### 7.4 推播 API

- `GET /ws/exchange`
  - 事件：`message.new`, `message.unreadCount`
- `GET /api/messages/stream`（可選 SSE）

#### WebSocket / SSE payload

`message.new`：
- `messageId`
- `title`
- `summary`
- `category`
- `severity`
- `createdAt`
- `isPinned`
- `actionUrl`（可為空）
- `isExpired`

`message.unreadCount`：
- `unreadCount`
- `byCategory`

### 7.5 模板 API（MVP）

- `GET /api/admin/messages/templates`
- `GET /api/admin/messages/templates/{templateCode}`
- 建立/修改模板先留待 v2。

### 7.6 錯誤碼

- VALIDATION_ERROR 400
- UNAUTHORIZED 401
- FORBIDDEN 403
- MESSAGE_NOT_FOUND 404
- MESSAGE_NOT_OWNED 403
- INVALID_CATEGORY 400
- INVALID_CHANNEL 400
- PREFERENCE_LOCKED 409
- SCHEDULE_NOT_CANCELABLE 409
- DUPLICATE_MESSAGE 200（建議冪等成功）
- INTERNAL_ERROR 500

---

## 8. 架構實作切片（供分工）

### Backend 1（核心）
- Message 核心模型、模板解析、去重鍵。
- 使用者訊息狀態寫入與 idempotent 寫入。
- 列表與詳情 API（cursor 分頁、搜尋、分類、status、archived）。
- 未讀數 API。

### Backend 2（權限與安全）
- 全量 ownership 驗證。
- admin/system 權限與 service token。
- 安全/法遵偏好鎖定。

### Backend 3（公告與事件）
- 公告建立、排程、取消。
- 系統事件 API。
- LAZY bulk 發送任務（大規模公告避免一次性 materialize）。

### Backend 4（即時）
- WS 推播（user channel）
- 重連同步策略
- unread-count cache

### Backend 5（可觀測與測試）
- 審計日誌：建立/推播/狀態變更。
- 驗證測試清單：
  - 僅能看自己訊息
  - 冪等操作
  - 批次已讀
  - 未讀統計
  - cursor 分頁不漏不重
  - 重連同步
  - 事件去重

---

## 9. 非功能需求

- 列表查詢須有索引支撐，禁止每次全表掃描。
- 大公告改用批次與延遲建立 user state。
- 留存原始訊息，不因刪除清除。
- 安全性：metadata 不得含敏感內部規則。
- 日誌與追蹤必須含 traceId + messageId。

## 10. 開發契約補強（前後端無歧義）

### 10.1 通用回應格式

所有 API 回應均使用 JSON 包裝：

- 成功
  - `{\"ok\": true, \"data\": <payload>, \"error\": null}`
- 失敗
  - `{\"ok\": false, \"data\": null, \"error\": {\"code\":\"ERROR_CODE\",\"message\":\"desc\",\"traceId\":\"uuid\"}}`

前端遇到 `ok=false` 一律走錯誤提示，除 `DUPLICATE_MESSAGE` 外不列重試視為成功邏輯。

### 10.2 Request/Path 取值邏輯

- 所有與 user 綁定的 API，`userId` 不得由前端送入，完全由 auth context 決定。
- 分頁 cursor 只允許 `createdAt desc + messageId desc` 向舊資料遞進，不做 offset。
- 前端每頁第一次請求使用 `cursor` 空值（或不帶）。
- 前端每頁成功拿到 `hasMore=true` 才繼續 `下一頁`，用 `nextCursor` 作為下一次參數。
- 若用戶切換搜尋/分類/status，前端應重新以空 cursor 查詢。

### 10.3 Cursor 定義（必看）

- Cursor 是 Base64URL 字串，解碼後至少包含：
  - `createdAt`: `ISO8601`
  - `messageId`: `UUID`
- 取得下一頁條件：
  - 回傳資料中的 createdAt/messageId 都要比 cursor 更舊（`createdAt <` OR `createdAt ==` 且 `messageId <`）。
- 前端不需要關心排序 SQL，只要保持客戶端順序照回傳順序 append 即可。

### 10.4 安全預設與鎖定邏輯

- `inAppEnabled` 只要是任何使用者都不准關閉。
- `SECURITY` 與 `COMPLIANCE` 分類：
  - inApp 必開
  - channel（email/sms/push）建議預設開啟且不可關閉（如果你要彈性，可在 admin 參數開關策略）。
- 若後台回傳 `locked=true`，前端必須禁用該開關輸入與互動。

### 10.5 一頁式前端行為序列（建議實作）

#### 首次進入訊息中心

1. 先呼叫 `GET /api/messages/unread-count` 顯示 badge。
2. 呼叫 `GET /api/messages`（空 cursor，預設 `status=ALL`，`archived=false`）。
3. 畫面呈現列表後，建立/續接 WS。
4. 列表卡片依 `isRead` 與 `isPinned` 顯示不同樣式。

#### 使用者點擊訊息

1. 呼叫 `GET /api/messages/{messageId}`。
2. 顯示正文、metadata、action 按鈕。
3. 立即呼叫 `POST /api/messages/{messageId}/read`。
4. 更新 local state：該筆卡片改為已讀，且 unread-count 重算。

#### 切換「未讀」與「已封存」

- `未讀`：`GET /api/messages?status=UNREAD&archived=false`
- `已封存`：`GET /api/messages?archived=true&status=ALL`
- `全部`：`GET /api/messages?status=ALL&archived=false`

#### 全部已讀

- `scope=ALL`：`POST /api/messages/read-all` body `{ "scope":"ALL" }`
- `scope=CATEGORY`：根據當前分類欄位傳入 `category`。
- 成功後務必 refresh：`unread-count` + 目前列表。

#### 即時事件處理

- 收到 `message.new`
  - 若目前是「未封存列表」且 `cursor=空`，可直接 prepend。
  - 若目前非第一頁，先顯示未讀新增提示（如「有新訊息」）。
- 收到 `message.unreadCount`：直接更新 badge 與未讀 tab badge。
- 重連流程：先重連 WS，再重新呼叫 `unread-count` + `messages`。

### 10.6 錯誤處理規則

- 400 `VALIDATION_ERROR`
  - 顯示欄位錯誤（如 `status`/`category` 非法），不做全局重試。
- 401 `UNAUTHORIZED`
  - 導向登入/重授權。
- 403 `FORBIDDEN` / `MESSAGE_NOT_OWNED`
  - 顯示無權限提示，不做重試。
- 404 `MESSAGE_NOT_FOUND`
  - 內容可能被刪除隱藏，提示「訊息不存在或已刪除」。
- 409 `PREFERENCE_LOCKED`、`SCHEDULE_NOT_CANCELABLE`
  - 顯示系統保護不可變更訊息，並保留原 UI 狀態。
- 重試策略
  - 5xx 可做 1 次指數退避重試（例如 500ms -> 1s），避免 duplicate（因為 API 為冪等）。
- 重複成功行為
  - 例如已讀/刪除再次呼叫，後端回 200，不變更結果。前端以回傳值為準，無需判斷是否為第一次。

### 10.7 API 最小可落地回傳樣板

1) 列表回傳範例  
`{"ok":true,"data":{"items":[{"messageId":"uuid-1","title":"BTC 入金完成","summary":"入金已到帳","category":"DEPOSIT","severity":"SUCCESS","createdAt":"2026-06-16T05:20:00.000Z","isRead":false,"isPinned":false,"isDeleted":false,"isArchived":false,"isExpired":false,"isScheduled":false,"actionUrl":"/wallet/deposit/uuid","actionLabel":"查看"}],"nextCursor":"eyJjcmVhdGVkQXQiOiIyMDI2LTA2LTE2VDA1OjIwOjAwLjAwMCIsIm1lc3NhZ2VJZCI6InV1aWQtMSJ9","hasMore":false}}`

2) 已讀回應範例  
`{"ok":true,"data":{"messageId":"uuid-1","isRead":true,"readAt":"2026-06-16T05:21:00.000Z"}}`

3) 偏好回應範例  
`{"ok":true,"data":{"preferences":[{"category":"DEPOSIT","inAppEnabled":true,"emailEnabled":true,"smsEnabled":false,"pushEnabled":true,"locked":false},{"category":"SECURITY","inAppEnabled":true,"emailEnabled":true,"smsEnabled":true,"pushEnabled":false,"locked":true}]}}`

### 10.8 非功能與觀測（前後端對齊）

- 後端 API 延遲 target（建議）：消息列表 p95 < 500ms（標準頁）。
- WS 重連 target：3 次內重連。
- 後端需輸出以下事件指標：
  - `message_api_latency_p95`
  - `message_push_queue_lag`
  - `message_dedup_hit_rate`
  - `message_read_error_count`
  - `message_ws_active_sessions`
- 這些欄位若未回傳，前端不得將值顯示為 0，而應視為未上線指標。

### 10.9 前端澄清與最終決策（不可再有歧義）

以下 14 點為本次最終落地規格，前端與後端皆以此為準：

1) 前端實作範圍
- 本任務前端只做「一般使用者訊息中心」三頁（列表、詳情、偏好），不含管理員公告管理頁。

2) API base path 與認證
- 全部 user/admin/system 皆以 `/api` 作為 API base。
- auth 使用同一機制（`Authorization: Bearer <token>`）。
- `/ws/exchange` 連線認證使用同一 token；前端不用傳 `uid`，由 token 解析綁定使用者。

3) `category` 參數格式
- 僅支援 repeatable：`category=ORDER&category=TRADE`。
- 前端不送 `category=ORDER,TRADE`，只送 repeatable。

4) 列表快取與 cursor 重設
- `search`、`category`、`status`、`archived` 任一變更，前端必重置 cursor 並清空頁面快取，重新查第一頁。

5) `status=UNREAD` + `archived`
- 兩者為交集邏輯（AND）：
  - `status=UNREAD` = 未讀
  - `archived=true` = 封存
- 範例：`status=UNREAD&archived=true` =「封存且未讀」。
- `unread-count` 是否排除封存由 `excludeArchived` 決定，預設 true。

6) cursor 不合法處理
- 前端遇到 cursor decode 失敗或後端回傳 invalid cursor：
  - 不視為錯誤阻斷
  - 清快取並 fallback 重抓第一頁（cursor 空）
  - 可提示「資料位移，已自動重載」。

7) message.new 去重
- `message.new` payload 必含 `messageId`。
- 前端以 `messageId` 去重，若訊息已在列表中，更新既有項目，不新增重複列。

8) 非第一頁新訊息顯示規則
- 第一頁：若符合目前視圖（未封存預設列表），可 prepend。
- 非第一頁：
  - 顯示「有新訊息」提示
  - 更新未讀 badge
  - 不直接改變列表內容。
- `archived=true` 視圖下原則上也以提示為主，不直接 prepend。

9) 詳情後 read 流程
- 前端必定呼叫 `GET /api/messages/{messageId}` 後再呼叫 `POST /api/messages/{messageId}/read`。
- 後端不在 detail API 自動置為已讀。

10) 已刪除 / 已封存 item 的 detail 行為
- 已刪除：detail 404（或 MESSAGE_NOT_FOUND）後，前端回到列表並提示「訊息不存在或已刪除」。
- 已封存：可正常查 detail；不影響明細頁展示。

11) PREFERENCE_LOCKED UI 行為
- `inAppEnabled` 永不關閉且固定顯示。
- `PREFERENCE_LOCKED` 僅鎖 email/sms/push。可操作範圍只剩 in-app 顯示，不可切換受鎖 channel。

12) byCategory 回傳結構
- 後端回傳完整 enum map（可包含 0 值），不採只回有值分類。

13) DUPLICATE_MESSAGE=200
- 前端視為成功 no-op，不顯示錯誤。
- 建議 toast 類型提示為「已是最新狀態」，僅在必要操作提示，不影響流程。

14) WS 重連
- 前端先自動重試 3 次（500ms、1s、2s）。
- 3 次皆失敗則顯示「連線中斷，點我重試」。
- 手動重試成功後，立即重新 sync：`unread-count` + 當前列表。

## 11. 上線驗收清單（後端 / 前端）

### 11.1 後端上線必備

- 身份與 ownership 全面通過（包括 user、admin、system）。
- Cursor 分頁無重複/漏資料。
- 去重命中率可觀測，且同事件不產生 duplicate message。
- 重複操作返回 200（冪等）。
- LAZY 公告不一次性掃描所有 user 入庫。
- WS/SSE 有用戶通道隔離與重連後補齊未讀邏輯。

### 11.2 前端上線必備

- 首次與 badge、列表、未讀 tabs 都可展示。
- 訊息列可顯示 `security` 類別特殊提示樣式。
- 支援三種視圖切換：全部、未讀、封存。
- 失敗場景有錯誤文案，且可離線重試。
- 收到 WS 後即時更新，斷線後手動或自動重新同步未讀數。

### 11.3 前端落地前確認（本文件最終版本）

- 已確認前端實作範圍與行為歧義已收斂：本任務僅做一般使用者訊息中心三頁（列表、詳情、偏好）。
- 前端實作可直接依 10.2~10.14 規格落地，不需再等待其他澄清。
- 任何後續開發請以「前一次 cursor 失敗需 fallback 首頁」與「非第一頁收到 message.new 只提示不 prepend」為強制規則。
- 風險提示：`/api` 與 WS 一律使用同一 Bearer token；`messageId` 為 list/detail 與 WS payload 對應主鍵，需做重複訊息去重。
- 實作順序建議：
  - 1) 列表 + cursor + filter + tabs
  - 2) 未讀 badge + 已讀/封存/釘選/刪除操作
  - 3) 詳情頁 + 偏好設定頁
  - 4) WebSocket 重連 + 新訊息提示 + 重同步
