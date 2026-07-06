# State Management

## Rule

Client state is not funds truth.

```text
Server balance projection -> frontend display
Frontend form estimate    -> preview only
Ledger truth              -> backend only
```

## Money display

- 最終金額計算不要使用 JavaScript 浮點數。
- 請使用 API 傳來的字串十進位值。
- Show asset 精度 consistently.
- Clearly separate available, held, pending withdrawal, and total.
