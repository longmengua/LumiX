# AI_START_HERE_MODEL_GATE_PATCH.md

> 將以下規則補到 `AI_START_HERE.md` 的「使用者互動規則」與「模型分級規則」之間。  
> 如果 repo 已經有 `AI_MODEL_GATE.md`，AI 每次「繼續開工」前必須先讀它。

---

## 模型開工閘門

每次執行「繼續開工」前，AI 必須先讀：

```text
AI_MODEL_GATE.md
```

並輸出模型開工檢查：

```text
Phase：
Task：
讀取文件：
任務等級：
建議模型：
建議 reasoning：
目前是否可執行：
本次允許範圍：
本次禁止範圍：
是否需要人工審查：
結論：
```

如果任務屬於 Level C 或 Level D，而目前模型或 reasoning 不符合要求，AI 必須停止，並提示使用者：

```text
目前任務屬於高風險核心，不建議用 mini medium 直接實作。
請切換 stronger model + high reasoning，或允許我只做 interface / stub / TODO。
```

AI 不得因為使用者只輸入「繼續開工」就跳過模型檢查。
