# P12 審查

## 審查清單

- [ ] All P12 tasks complete.
- [ ] Migration order is clear.
- [ ] Schema can be applied from scratch.
- [ ] Precision rules are explicit.
- [ ] 帳本 tables support append-only behavior.
- [ ] 預留 states are representable.
- [ ] 訂單 lifecycle states are representable.
- [ ] Wallet deposit and withdrawal states are representable.
- [ ] Outbox, audit, and idempotency are present.
- [ ] No runtime money movement was added.

## 驗證策略

```text
1. Run `./mvnw -Dtest=P12T02SchemaMigrationTest,P12T03SchemaMigrationTest,P12T04SchemaMigrationTest,P12T05SchemaMigrationTest,P12T06SchemaMigrationTest,P12T07SchemaMigrationTest,P12T08SchemaMigrationTest,P12T09SchemaVerificationTest test`.
2. Replay the SQL migrations against a local PostgreSQL container with `psql` when container access is available.
3. Check PostgreSQL metadata comments, FK coverage, and exact numeric types on money / price columns.
```

## PostgreSQL fallback

```text
Testcontainers was not added for this phase because the workspace does not have cached PostgreSQL / Testcontainers dependencies.
Use a local PostgreSQL container and `psql` replay as the explicit alternative verification path:
  docker run --rm --name lumix-pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=lumix -p 5432:5432 postgres:16-alpine
  for f in server/src/main/resources/db/migration/V00*.sql; do
    docker exec -i lumix-pg psql -U postgres -d lumix -f - < "$f"
  done
  docker exec -i lumix-pg psql -U postgres -d lumix -c "\\dt"
```

## Final review gate

```text
- P12-T01 到 P12-T10 的狀態已逐一收斂為 completed。
- migration sequence 已明確標示 V005 提前建立 wallet lifecycle schema，V006 補齊 reservation schema。
- Phase 12 schema suite 已以 H2 + Flyway 驗證通過；P12-T05 的 comment expectation 已同步到已發布 migration 的實際字串。
- ledger 仍是 source of truth；balance_projections 僅是 read model。
- reservations、orders / trades、wallet lifecycle、outbox / idempotency / audit 皆維持 schema-only，未加入 runtime service。
- 仍需真 PostgreSQL replay 與後續 runtime / security review，故不得宣稱 production-ready。
```

## 人工簽核

Reviewer:
日期：
決定: GO / NO-GO
Notes:
