# 交易所訊息中心 API 規格（後端 v1）

最終更新：2026-06-16

## 1. 契約總覽（規範）

- 入口：`/api/messages/*`（使用者）、`/api/admin/messages/*`（管理員）、`/api/system/messages/*`（內部服務）。
- 即時更新：建議使用既有 `/ws/exchange` WebSocket；另可擴充 SSE。
- 成功回應使用統一封包：
  - `200`：`ApiResponse<T> = { "ok": true, "data": T, "error": null }`
  - `200` 對於刪除/標記已讀/重複操作必須保持冪等：再次呼叫回傳成功，不可 4xx。
- 失敗回應使用：
  - `ErrorResponse = { "ok": false, "error": { "code": "ERROR_CODE", "message": "描述", "traceId": "uuid" } }`
- `uid` 嚴禁由 request body/query 提供，必須從認證主體推斷。
- 時間統一 UTC ISO-8601（含毫秒），例如 `2026-06-16T07:30:12.123Z`。
- 資料頁面為多租戶個人資料，必須做 ownership check：訊息與偏好只允許操作本人資料。
- 全站公告不一定立即寫入所有 user state，可使用 lazy materialization / 批次任務。

## 1.1 安全與授權邊界

- 一般 API：登入使用者 context 驗證。
- 後台 API：必須具備 `ADMIN` 權限。
- 內部事件 API：系統服務憑證 + 來源驗證（service-to-service auth）。
- 所有訊息 ID 操作，必須以 `messageId + request principal` 驗證所有權。

## 2. 共用欄位定義

### 2.1 列舉欄位

- `MessageCategory`
  - `SYSTEM`, `ANNOUNCEMENT`, `ORDER`, `TRADE`, `DEPOSIT`, `WITHDRAW`, `ACCOUNT`, `SECURITY`, `PROMOTION`, `COMPLIANCE`
- `MessageSeverity`
  - `INFO`, `SUCCESS`, `WARNING`, `CRITICAL`
- `DeliveryChannel`
  - `IN_APP`, `EMAIL`, `SMS`, `PUSH`
- `AudienceType`
  - `ALL_USERS`, `USER_IDS`, `VIP_LEVEL`, `HAS_ASSET`, `CUSTOM_FILTER`
- `DeliveryMode`
  - `DIRECT`, `LAZY`
- `MessageStatus`
  - `UNREAD`, `READ`, `ARCHIVED`, `DELETED`

### 2.2 訊息物件

- `MessageItem`
  - `messageId` (UUID)
  - `templateCode` (string)
  - `title` (string)
  - `summary` (string)
  - `category` (MessageCategory)
  - `severity` (MessageSeverity)
  - `createdAt` (ISO8601)
  - `updatedAt` (ISO8601)
  - `readAt` (ISO8601, nullable)
  - `isRead` (boolean)
  - `isArchived` (boolean)
  - `isDeleted` (boolean)
  - `isPinned` (boolean)
  - `isExpired` (boolean)
  - `isScheduled` (boolean)
  - `actionUrl` (string, nullable)
  - `actionLabel` (string, nullable)
  - `metadata` (object)
  - `createdByAdmin` (boolean)
  - `securityLevel` (string: `NORMAL`, `SENSITIVE`)
  - `dedupeKey` (string)

- `MessageListResult`
  - `items` (array of `MessageItem`)
  - `nextCursor` (string, nullable)
  - `hasMore` (boolean)
  - `limit` (int)

- `NotificationPreference`
  - `category`
  - `inAppEnabled` (boolean, 必為 true)
  - `emailEnabled` (boolean)
  - `smsEnabled` (boolean)
  - `pushEnabled` (boolean)
  - `securityImmutable` (boolean, 由 server 設定)

## 3. Cursor 分頁規格

- 分頁參數：`cursor`, `limit`
- 排序：`createdAt desc, messageId desc`
- Cursor 內容使用字串，表示「最後一筆訊息位點」：
  - base64url 格式，解碼後為 `{"createdAt":"2026-06-16T10:00:00Z","messageId":"..."}`
- 查詢條件為「比 cursor 更舊」(createdAt, messageId 全小於當前位點)
- 回傳 200 時同時回 `nextCursor`，前端用於繼續讀取下一頁
- 不允許 offset/頁碼。

## 4. 使用者 API

### 4.1 取得訊息列表

- `GET /api/messages`
  - Query
    - `cursor` (optional)
    - `limit` (optional, default `30`, max `100`)
    - `status` (optional, `UNREAD|ALL`)
  - `archived` (optional, `true|false`, default `false`)：預設只看非封存
  - `search` (optional, 關鍵字比對標題與內文)
  - `category` (optional, repeatable 或逗號串列)
  - `pinnedFirst` (optional, default `true`)
  - `excludeDeleted` (optional, default `true`)
  - Response `MessageListResult`

  - 查詢建議回傳欄位（每筆）：
    - `messageId`, `title`, `summary`, `category`, `severity`, `createdAt`, `isRead`, `isPinned`, `actionUrl`, `isExpired`, `isDeleted`, `isArchived`

### 4.2 取得訊息詳情

- `GET /api/messages/{messageId}`
  - Response `MessageItem`

### 4.3 標記已讀

- `POST /api/messages/{messageId}/read`
  - Response `{ "ok": true, "data": { "messageId": "...", "isRead": true, "readAt": "..." } }`
  - 重複操作回應需為成功且 `isRead=true`。

### 4.4 批次標記已讀

- `POST /api/messages/read-all`
- Request
  - `{ "scope": "ALL|CATEGORY", "category": "ORDER" }`
- Response
  - `{ "updatedCount": 0 }`

### 4.5 刪除（僅隱藏）

- `DELETE /api/messages/{messageId}`
- Response `{ "deleted": true, "messageId": "..." }`
- 僅影響使用者可見性，不得刪除原始訊息正文與稽核資料。

### 4.6 封存

- `POST /api/messages/{messageId}/archive`
- `POST /api/messages/{messageId}/unarchive`
- Response `{ "isArchived": true/false }`

### 4.7 釘選

- `POST /api/messages/{messageId}/pin`
- `DELETE /api/messages/{messageId}/pin`
- Response `{ "isPinned": true/false }`

### 4.8 未讀數

- `GET /api/messages/unread-count`
  - Query
    - `excludeArchived` (optional, default `true`)
  - Response
  - `unreadCount` (int)
  - `byCategory` (map<MessageCategory, int>)

### 4.9 通知偏好

- `GET /api/message-preferences`
  - Response `NotificationPreference[]`
- `PUT /api/message-preferences`
  - Request `NotificationPreference[]`
  - SECURITY / COMPLIANCE 分類不允許關閉 `IN_APP`，也不能關閉 `email`/`sms`/`push`（以系統策略為主）
  - 其他分類可依規則設定 `emailEnabled/smsEnabled/pushEnabled`。

## 5. 全域規則（使用者訊息）

- 使用者無法直接改變他人訊息狀態（含已讀/釘選/封存/刪除）。
- `UNREAD` 不包含已封存、已刪除訊息（除非特別查詢 `archived=true`）。
- 已封存訊息不顯示於「全部」與「未讀」預設列表；需透過 `archived=true` 查詢才顯示。

## 6. 管理員公告 API

### 6.1 建立公告

- `POST /api/admin/messages/announcements`
- Request
  - `title`
  - `summary`
  - `category` (`ANNOUNCEMENT | PROMOTION | SYSTEM`)
  - `severity`
  - `templateCode`
  - `templateVars`（object）
  - `actionUrl`（optional）
  - `channels`（`IN_APP`/`EMAIL`/`SMS`/`Push`）
  - `audience`
    - `type`
    - `userIds`
    - `vipLevels`
    - `minVipLevel`
    - `hasAssetSymbol`
    - `assetSymbol`
    - `customFilter`（JSON query）
  - `sendAt`（optional，null 表示即刻）
  - `expireAt`（optional）
  - `deliveryMode`（`DIRECT` / `LAZY`）
  - `dedupeKey`（optional）
- Response
  - `announcementId`
  - `status` (`PUBLISHED`/`SCHEDULED`)
  - `estimatedRecipients`

### 6.2 取消排程公告

- `POST /api/admin/messages/announcements/{announcementId}/cancel`
- 僅允許 `status = SCHEDULED`

### 6.3 查詢公告

- `GET /api/admin/messages/announcements`
  - Query: `status`, `category`, `from`, `to`, `cursor`, `limit`
  - Response: 分頁結果

### 6.4 查詢公告明細

- `GET /api/admin/messages/announcements/{announcementId}`
- Response 含發送/排程/失敗統計

## 7. 系統事件 API（內部服務）

### 7.1 單一事件發送訊息

- `POST /api/system/messages/event`
- Request
  - `eventType`
  - `eventId`
  - `eventTimestamp`
  - `dedupeKey`
  - `users`（`[{uid, metadataOverrides?}]`）
  - `templateCode`
  - `templateVars`
  - `category`、`severity`
  - `actionUrl`
  - `channels`（optional, default `IN_APP`）

### 7.2 批次事件發送

- `POST /api/system/messages/event/batch`
- body: `events` array
- 回傳逐筆結果：`{"messageId":..., "uid":..., "status":"created|skipped|duplicate"}`

### 7.3 指定使用者清單發送

- `POST /api/system/messages/send`
- Request
  - `uids`
  - `templateCode`
  - `templateVars`
  - `category`
  - `severity`
  - `actionUrl`
  - `dedupeKey`
- Response
  - `acceptedCount`
  - `dedupeSkippedCount`

## 8. 模板 API（可先做只讀）

### 8.1 查詢可用模板

- `GET /api/admin/messages/templates`
- Response: templateCode 列表

### 8.2 取得/建立模板（MVP 可落成只讀）

- `GET /api/admin/messages/templates/{templateCode}`
- `POST /api/admin/messages/templates`（未來擴充）
- `PUT /api/admin/messages/templates/{templateCode}`（未來擴充）

## 9. 即時通知（WebSocket / SSE）

### 9.1 WebSocket

- Connect: `GET /ws/exchange`
- 首次 subscribe（必須）：
  - `{ "type": "subscribe.user", "uid": 123, "authorization": "Bearer ...", "apiKey": "...", "resumeConnectionId": "...", "cancelOnDisconnect": false }`
- 服務器回應：
  - `event: subscribed.user`
  - `data: { "uid": 123, "connectionId": "uuid" }`
- 推送訊息：
  - `event: message.new`
  - `data: { "messageId": "...", "title": "...", "summary": "...", "category": "ORDER", "severity": "INFO", "createdAt": "...", "isPinned": false }`
  - `event: message.unreadCount`
  - `data: { "unreadCount": 12, "byCategory": { "ORDER": 3, "SECURITY": 2 } }`
- 斷線重連建議客戶端重新連線後呼叫
  - `GET /api/messages/unread-count`
  - `GET /api/messages?status=UNREAD`

### 9.2 SSE（可選）

- `GET /api/messages/stream`
- 事件類型
  - `message.new`
  - `message.unreadCount`
- 需要 `Authorization`。

## 10. 錯誤碼

- `MESSAGE_NOT_FOUND` -> 404
- `MESSAGE_NOT_OWNED` -> 403
- `MESSAGE_ALREADY_DELETED` -> 200（可設計為冪等成功）
- `INVALID_CATEGORY` -> 400
- `INVALID_CHANNEL` -> 400
- `PREFERENCE_LOCKED` -> 409（e.g. SECURITY/COMPLIANCE 不可關閉）
- `DUPLICATE_MESSAGE` -> 200（或 409，建議回 200 作冪等）
- `SCHEDULE_NOT_CANCELABLE` -> 409
- `UNAUTHORIZED` -> 401
- `FORBIDDEN` -> 403
- `VALIDATION_ERROR` -> 400
- `INTERNAL_ERROR` -> 500

### 10.1 錯誤行為規則

- 參數錯誤必回 `400` 並帶 `VALIDATION_ERROR`，回報欄位名稱與失敗原因。
- 權限不足：`401`（未授權）或 `403`（權限不足）。
- 重複行為：
  - 已讀、刪除、封存、取消封存、釘選、取消釘選皆可重複成功（冪等）。
  - `DUPLICATE_MESSAGE` 建議以 `200` + `{ "skipped": true }` 回傳。

## 11. 冪等與去重

- 所有寫入類 API（訊息建立、標記已讀、封存、釘選、刪除、偏好更新）需冪等。
- 建立訊息時須接受 `dedupeKey`：`{eventType}:{eventId}:{uid}`。
- `SYSTEM`/`COMPLIANCE` 相關通知不能被使用者關閉站內通知。

## 12. 全站公告壓力處理建議

- `deliveryMode=LAZY` 時先寫公告主體與排程/批次任務，不同步為每位使用者建立 `user_message_state`。
- 實際發送時按批次掃描活躍使用者並建立使用者訊息關聯。
- `estimatedRecipients` 僅為估算值。

## 13. 未來擴充（非 v1 必要）

- 依 `templateCode` 追溯 `DeliveryLog`。
- 依 `audience.customFilter` 加上審計欄位（規則 hash）。
- 新增 `message.deleted` 審計事件。
- SSE/WS 通道監控指標與權限審計。

## 14. 供前端實作的最小契約

- 列表使用 cursor 分頁，不得使用 offset/page；新增資料不會回頁或重複資料。
- `status=UNREAD` 必須只回傳 `isRead=false` 且未刪除訊息。
- 釘選訊息需在列表第一頁保留在最前面（若頁面大小允許，否則分頁邏輯仍需穩定）。
- 取列表、取未讀數、取明細都會做 N+1 防護；後端允許透過索引查詢避免全表掃描。
- 明細與列表在「安全/法遵」類別需加高強度提示（前端必須視覺化）。
