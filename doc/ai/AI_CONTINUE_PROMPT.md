# AI_CONTINUE_PROMPT.md

> 用途：之後你可以把這段丟給 Codex，或直接輸入「繼續開工」。  
> 若 Codex 沒有自動讀文件，請貼本文件內容給它。

---

## 繼續開工指令

```text
請閱讀 AI_START_HERE.md 與 AI_PROGRESS.md，然後繼續開工。

執行規則：
1. 先確認目前 Phase 與 current_task。
2. 如果上一個 Phase 尚未人工審查通過，不要繼續，請要求人工審查。
3. 如果可以繼續，選擇下一個 pending 任務。
4. 讀取該任務對應的 doc/ai_backend 或 doc/ai_frontend 文件。
5. 先判斷任務等級：Level A / B / C / D。
6. 嚴格遵守等級限制。
7. 只完成本次任務，不要跨 Phase。
8. 不要重構無關檔案。
9. 完成後更新 AI_PROGRESS.md。
10. 回報完成摘要、修改檔案、測試方式、TODO、風險與下一步。

重要限制：
- 不得直接修改用戶資產餘額。
- 不得自行實作未審查的撮合、強平、保證金、PnL、槓桿風險率。
- 不得處理真實私鑰或鏈上出帳。
- 不得明文保存 API secret。
- 所有高風險邏輯必須標記 TODO: requires high-reasoning review。
```
