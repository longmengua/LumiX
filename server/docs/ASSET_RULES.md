# ASSET_RULES

- Every asset change must go through the ledger service interface.
- Do not directly modify `balance`, `total`, `available`, or `locked`.
- Every asset-changing request must carry a `requestId` or `idempotencyKey`.
- Phase 9 must not implement real debit, credit, or settlement logic.
- Spot, futures, and margin balances must not share one generic account bucket.
