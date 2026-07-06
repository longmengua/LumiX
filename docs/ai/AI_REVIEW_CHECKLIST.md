# AI Review Checklist

## Scope

- [ ] Did the change stay inside the assigned task?
- [ ] Did it avoid later phases?
- [ ] Did it avoid hidden production claims?

## Safety

- [ ] Money values avoid floating point.
- [ ] Ledger remains append-only.
- [ ] Balance projection is not source of truth.
- [ ] Idempotency is present for effectful operations.
- [ ] High-risk change has `HUMAN_REVIEW_REQUIRED`.

## Documentation

- [ ] Phase task status updated.
- [ ] Migration notes updated if schema changed.
- [ ] Pure text diagrams only.

## Verification

- [ ] Tests or validation commands listed.
- [ ] Failure mode considered.
- [ ] Rollback or repair notes provided where relevant.
