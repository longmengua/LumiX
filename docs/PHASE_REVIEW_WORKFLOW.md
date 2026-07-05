# Phase 審核流程

這份文件說明每一個章節怎麼從「正在做」走到「正式完成」。它的目的很簡單：先把事情做對，再把狀態改對。

## 標準流程

1. 發布一個 Phase 指令。
2. Codex 重新讀取 repo。
3. Codex 只看當前章節需要的文件。
4. Codex 只做當前章節，不偷做下一章。
5. Codex 完成後先做建置與測試。
6. Codex 先標記為 `implementation completed / pending human review`。
7. 人工檢查實際改動。
8. 人工明確批准，句子只能是：
   - `Phase X 人工審核完成`
   - `Phase X human review approved`
   - `Approve Phase X completion`
9. 收到批准後，才可以改成 `completed`。
10. 再把結果交回下一輪，由下一輪決定下一個章節。

## Codex 不能做的事

- 不能自動把章節標成 `completed`。
- 不能跳到下一個章節。
- 不能把 stub、interface、mock、placeholder 或 TODO 當完成。
- 不能在 Phase 36 前宣稱正式上線就緒。
- 不能在撮合、凍結、帳本、結算未完成前宣稱正式交易完成。
- 不能在沒有人工批准前提交 completed 狀態。

## 狀態上限

- 在人工批准前，最高只能到 `implementation completed / pending human review`。

## 章節來源

- `docs/OPERATING_EXCHANGE_MASTER_PLAN.md`
- `docs/phases/`
