# AI Agent Operating Manual

本文件給 Codex、mini、review agent 與架構 agent 使用。

## Roles

```text
+------------------+       +------------------+       +------------------+
| Human reviewer   | ----> | Architect agent  | ----> | Mini engineer    |
| final authority  |       | design boundary  |       | task execution   |
+------------------+       +------------------+       +------------------+
          ^                         |                         |
          |                         v                         v
          |               +------------------+       +------------------+
          +---------------| Review agent     | <-----| Code / docs diff |
                          | gate checking    |       | implementation   |
                          +------------------+       +------------------+
```

## Agent responsibilities

### 架構師 agent

- 拆分文件。
- 定義領域邊界。
- 審核高風險流程。
- 製作純文字 UML / 架構圖。
- 給 mini 可執行任務卡。

### Mini 工程師

- 只執行單一任務卡。
- 不重設架構。
- 不擴大 scope。
- 遇到帳本、錢包、撮合、結算、資金安全分歧時停止並回報。
- 任何新增或修改的程式碼都必須符合 `docs/engineering/code-commenting-standard.md`。

### Review agent

- 檢查是否跳階。
- 檢查是否把 stub 當完成。
- 檢查是否違反資金安全不變式。
- 檢查是否缺測試或驗證方式。

## 節省 Token 流程

每次任務只載入四層文件。

```text
Layer 0: AGENTS.md / AI_AGENT.md
Layer 1: docs/ai/AI_CONTEXT_ROUTING.md
Layer 2: domain README, e.g. docs/exchange-core/README.md
Layer 3: phase task list or task card, e.g. docs/phases/PHASE_17_ORDER_INTAKE/README.md
```

禁止在沒有必要時讀取：

```text
all docs files
all phase files
all product docs
all architecture docs
```

## 給 Codex 的目前工作指引

開始 第 17 階段 時，請照順序執行：

```text
1. Read AGENTS.md
2. Read AI_PROGRESS.md
3. Read docs/ai/AI_CONTEXT_ROUTING.md
4. Read docs/phases/PHASE_17_ORDER_INTAKE/README.md
5. Pick the first unchecked task listed in docs/phases/PHASE_17_ORDER_INTAKE/README.md
6. Implement only that task
7. Run the narrowest useful tests
8. Update the task note or status and notes
9. Stop and produce a review summary
```

## 人工審核觸發條件

下列任一條件發生時，必須標記 `HUMAN_REVIEW_REQUIRED`：

```text
ledger mutation
balance projection mutation
reservation hold/release/capture rules
order matching rules
settlement rules
fee rounding rules
withdrawal approval / signing / broadcast
deposit confirmation crediting
admin balance adjustment
risk-control bypass
authentication or authorization change
secret management change
```

## 程式碼註解規則

- 所有新增或修改的程式碼都必須具備足夠註解，以利後續維護。
- 註解使用繁體中文，台灣用語。
- 標準技術術語保留英文，例如 migration、ledger、reservation、matching、settlement、idempotency、outbox。
- 不要求每一行都加註解，也不寫重複程式碼內容的噪音註解。
- 註解應說明為什麼這樣做、業務限制、安全限制、資料一致性限制與後續維護注意事項。
- 高風險領域要有更完整註解，包括 ledger、balance、reservation、withdrawal、matching、settlement、PnL、liquidation、risk、admin、security、idempotency、outbox、reconciliation。
- SQL migration 必須包含 migration 檔頭說明、table / column inline comments，以及 PostgreSQL `COMMENT ON TABLE` / `COMMENT ON COLUMN`。
- Java 程式在 `public class`、`public method`、複雜 business rule、transaction boundary、高風險流程要補 JavaDoc 或區塊註解。
- TypeScript / React 程式在複雜 hook、狀態流、權限判斷、交易表單驗證、API boundary 要補 JSDoc 或區塊註解。
- 測試程式要註解測試意圖，尤其是資金、安全、資料一致性與錯誤案例。
- 如果需要大量註解才能理解，應優先重構或拆小函式，不可用註解掩蓋壞設計。

## Mini 輸出格式

完成任務後輸出：

```text
Task: P17-Txx
Files changed:
  - path
What changed:
  - concise summary
Verification:
  - command or manual check
Risk:
  - none / low / HUMAN_REVIEW_REQUIRED
Next suggested task:
  - P17-Tyy
```
