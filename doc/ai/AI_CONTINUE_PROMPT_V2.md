# AI_CONTINUE_PROMPT_V2.md

## 繼續開工指令

```text
請閱讀 AI_START_HERE.md、AI_MODEL_GATE.md、AI_PROGRESS.md，然後繼續開工。

開工前必做：
1. 先根據 AI_MODEL_GATE.md 做模型開工檢查。
2. 判斷當前任務是 Level A / B / C / D。
3. 判斷目前模型與 reasoning 是否符合任務要求。
4. 如果不符合，停止並回報需要切換模型或只能做 stub。
5. 如果符合，才可以開始修改檔案。

執行規則：
1. 只完成目前 Phase 的下一個 pending 任務。
2. 不要跨 Phase。
3. 不要重構無關檔案。
4. 不要新增大型套件。
5. 不要直接修改資產餘額。
6. 不要實作未審查的撮合、強平、保證金、PnL、槓桿風險率。
7. 不要處理真實私鑰或鏈上出帳。
8. 所有高風險核心必須標記：TODO: requires high-reasoning review before production use。
9. 完成後更新 AI_PROGRESS.md。
10. 完成後回報修改檔案、測試方式、TODO、風險、是否需要人工審查。
```
