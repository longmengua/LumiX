# AI 審查檢查清單

## Scope

- [ ] Did the change stay inside the assigned task?
- [ ] Did it avoid later phases?
- [ ] Did it avoid hidden production claims?

## Safety

- [ ] Money values avoid floating point.
- [ ] 帳本 remains append-only.
- [ ] 餘額投影 is not source of truth.
- [ ] Idempotency is present for effectful operations.
- [ ] High-risk change has `HUMAN_REVIEW_REQUIRED`.

## Documentation

- [ ] 階段 任務狀態更新d.
- [ ] Migration notes updated if schema changed.
- [ ] Pure text diagrams only.

## Verification

- [ ] Tests or validation commands listed.
- [ ] Failure mode considered.
- [ ] Rollback or repair notes provided where relevant.
