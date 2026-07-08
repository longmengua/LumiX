# Phase 14 - Ledger Posting Command Boundary

## 目的

這份文件只描述 ledger posting command boundary 的 application-level 流程。
它不代表正式 posting runtime，也不代表 ledger 已可寫入資料庫。

## Boundary sequence

```text
command received
  -> validate requestId and journal draft
  -> verify runtime prerequisites
  -> validate journal invariant
  -> build append-only posting plan
  -> return accepted plan or rejected result
```

## Accepted plan semantics

```text
accepted 只表示 command 已通過 prereq / invariant gate
plan 只表示後續若要進入正式寫入，應使用哪些 mapping contract
accepted 不表示資料已寫入
accepted 不表示 balance 已變更
accepted 不表示任何 outbox 或 side effect 已發生
```

## Rejected result semantics

```text
rejected 必須只輸出安全原因
rejected 不得外洩 SQL、stack trace、secret 或底層 client 例外
rejected 不得假裝已完成資料寫入
```

## 後續正式 gate

```text
application command boundary
  -> append transaction boundary design
  -> persistence contract review
  -> transaction boundary review
  -> DB write implementation review
  -> reconciliation / audit review
```

## HUMAN_REVIEW_REQUIRED

```text
任何把這份 command boundary 接到正式 ledger runtime 的變更都屬於 HUMAN_REVIEW_REQUIRED。
任何會讓 accepted plan 被誤解為已完成資金異動的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
