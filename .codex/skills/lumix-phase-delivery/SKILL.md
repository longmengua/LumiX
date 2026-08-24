---
name: lumix-phase-delivery
description: "依 LumiX phase 順序完成 task、更新階段證據，並在每個完整 phase 後以獨立 commit 與 push 交付。適用於 LumiX 後續 phase 實作；不適用於單一臨時修正。"
---

# LumiX Phase Delivery

以「一個完整 Phase、一份可稽核交付」為目標，而不是把多個 Phase 混入同一次遠端推送。

## 施工前

1. 先讀 `AGENTS.md`、`AI_AGENT.md`、`AI_PROGRESS.md`、`docs/ai/AI_CONTEXT_ROUTING.md`、目前 phase README 與下一張 task 的相依內容。
2. 依 phase 順序完成 task；逐卡 approve 暫停時仍不得跳 phase、跳 task dependency 或跨越禁用範圍。
3. money movement、ledger/balance mutation、wallet signing、provider secret、security bypass 與 production claim 仍受專案規則限制；本 skill 不提供額外授權。

## 每張 task

- 僅實作該 task 定義的 contract、runtime 或文件範圍，並保留繁體中文的必要程式碼註解。
- 執行聚焦測試；完成 task 時更新 phase README 與 `AI_PROGRESS.md` 的實際狀態。
- 不因 task 完成就 push。若 phase 尚有 task，保留本機提交，避免把未完成 phase 當作獨立 release。

## Phase 交付

每個 phase 的最後一張 task 完成後：

1. 建立 phase final review，明確列出完成 foundation、驗證、未完成/no-claim、rollback 與下一 phase。
2. 執行 `git diff --check` 與完整 server test；記錄實際測試數與 skip。
3. 先執行 `git status --short`。若有未相關變更，僅 stage 本 phase 的明確路徑，絕不順帶提交。
4. 提交一則清楚標示 phase completion 的 commit；既有 task commits 可以存在，但遠端交付須停在該 phase final commit。
5. 將 remote 分段快轉到該 phase final commit，確認 push 成功與遠端狀態。不得把下一個未完成 phase 一併 push。
6. 更新 `AGENTS.md`、`AI_AGENT.md`、`AI_PROGRESS.md`、context routing 與下一 phase README，使後續 agent 的入口一致。

## 回報格式

回報應先說明已完成的 phase、commit 與 push 狀態，再列出完整驗證結果與仍被禁止的 runtime/money boundary。若下一 phase 已開始但未完成，明確說明它只保留在本機、尚未 push。
