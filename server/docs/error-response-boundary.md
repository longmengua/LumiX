# Error Response Boundary

## 目的

本文件定義 LumiX 後端共用的錯誤回應格式與 exception boundary。
這一層的重點是對外回傳一致、可追蹤、不可洩漏敏感資訊的錯誤資訊，而不是把所有內部例外原封不動吐出去。

## 錯誤回應格式

```text
{
  code: "VALIDATION_ERROR",
  message: "請求驗證失敗",
  requestId: "req-123",
  timestamp: "2026-07-07T12:34:56Z",
  details: { ... } // optional
}
```

## 欄位說明

- `code`：穩定的機器可讀錯誤碼。
- `message`：給前端或 API 使用者看的安全訊息，不放 stack trace。
- `requestId`：要求每筆錯誤都要可追蹤。
- `timestamp`：錯誤發生時間，使用 UTC 風格的 ISO-8601 表示。
- `details`：可選，只有在內容已去敏且可公開時才放。

## 錯誤碼分類

```text
VALIDATION_ERROR
AUTHENTICATION_ERROR
AUTHORIZATION_ERROR
NOT_FOUND
CONFLICT
RATE_LIMITED
INTERNAL_ERROR
HIGH_RISK_OPERATION_REJECTED
```

## Validation mapping

- validation exception 轉成 `VALIDATION_ERROR`。
- `details` 建議使用 `violations` 陣列，每筆只保留安全欄位。
- `field` 與 `reason` 可以公開。
- `rejectedValue` 預設不公開，只有明確允許的安全情境才可帶出。

## 高風險原則

- ledger、withdrawal、settlement、risk 類錯誤不得被包裝成一般 validation error 來淡化風險。
- 若屬於高風險操作拒絕，錯誤分類要保留 `HIGH_RISK_OPERATION_REJECTED`，並在程式註解或測試意圖中標記 `HUMAN_REVIEW_REQUIRED`。
- 不在 response 中暴露任何內部簽章、wallet 路徑、SQL 或 secret。

## 文字圖

```text
+------------------+     +------------------------+     +---------------------+
| Controller/API   | --> | Exception boundary     | --> | ApiErrorResponse     |
+------------------+     +------------------------+     +---------------------+
          |                          |
          |                          +--> sanitize / map / classify
          v
   RequestId / correlationId
```

## Phase 13 原則

- 先把 response contract 固定，再讓後續 API layer 去接。
- 不在此階段加入真正的 controller 或 web routing。
- 不把內部 exception message 當成公開 message。
