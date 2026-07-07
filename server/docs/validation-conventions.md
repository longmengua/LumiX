# Validation Conventions

## 目的

本文件定義 API validation 的共用規則。
驗證的目的不是把內部規則塞進 controller，而是確保 request 在進入 application 之前，已經被安全地分流與去敏。

## 核心原則

- 驗證錯誤必須轉成可公開的 `ApiErrorResponse`。
- validation details 只能保留安全欄位。
- `rejectedValue` 是否公開必須保守處理，預設不公開。
- 不把 stack trace、SQL、secret、private key 或 wallet signing 資訊放入 validation response。

## 建議 validation detail 形狀

```text
{
  field: "amount",
  reason: "must be greater than zero",
  rejectedValue: "optional and redacted by default"
}
```

## 高風險欄位

```text
amount
price
quantity
address
requestId
clientOrderId
```

這些欄位在 future DTO 中要特別補維護性註解，因為它們常牽涉資金、身份追蹤或下單冪等。

## 文字圖

```text
+------------------+     +-----------------------+     +---------------------+
| request DTO      | --> | validation exception  | --> | ApiErrorResponse    |
+------------------+     +-----------------------+     +---------------------+
```

## Phase 13 原則

- 先統一 validation contract，再接 web framework 的 annotation。
- 不在這一階段實作 runtime 邏輯或交易流程。
