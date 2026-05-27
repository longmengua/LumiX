<!-- 檔案用途：撮合引擎模塊說明文件，預留 in-memory matching 實作與演進筆記。 -->
# 撮合引擎

本模塊包含目前的 in-memory 撮合實作，適合本機開發與 MVP 驗證。Production 前仍需要把 command log、event log、snapshot、offset checkpoint 與 replay validation report 改成 durable storage。

目前檔案：

- `InMemoryMatchingEngine`：實作 `MatchingEngine` contract 的撮合 adapter。
- `InMemoryMatchingCommandLog`：供 replay 測試使用的 in-memory command log baseline。
- `InMemoryMatchingEventLog`：供 replay validation 使用的 in-memory trade event log baseline。
- `JpaMatchingCommandLog` / `JpaMatchingEventLog`：Spring wiring 使用的 durable JPA log adapter。
- `JpaMatchingSnapshotStore`：matching engine snapshot 的 durable JPA store。
- `JpaMatchingReplayValidationReportStore`：replay validation audit report 的 durable JPA store。
- `README.md`：英文說明。

目前狀態：

- 單 symbol 操作會由 in-process sequencer thread 序列化。
- LIMIT / MARKET、GTC / IOC / FOK、post-only、自成交防護、amend、cancel、snapshot 都有測試覆蓋。
- Snapshot export/restore 已攜帶 command offset 與 event offset。
- Replay 可用 snapshot checkpoint 加後續 command log 重建單一 symbol 狀態。
- Replay validation 會比對 command offset、event offset、match sequence 與聚合後的 book levels。
- Command/event log、snapshot 與 replay validation report 已有 durable schema、JPA adapter baseline 與 per-symbol offset checkpoint。
- Startup / worker-takeover recovery orchestration 仍是 TODO。
