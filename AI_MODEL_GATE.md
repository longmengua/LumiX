# AI_MODEL_GATE.md

> 用途：這是 Codex / AI 每次「繼續開工」前必須先執行的模型能力檢查。  
> 目的：避免低 reasoning 或 mini model 自行實作高風險交易核心，例如資產帳本、撮合、合約強平、保證金、PnL、槓桿風險率、錢包出帳。  
> 前端固定為 web/ React + TypeScript + Vite。後端固定為 Java 21 + Spring Boot 3，程式碼只放 `server/`。C++ Core 是 OL 前必要項，程式碼必須放在 `core/` 或 `matching-core/`，不可缺位。

---

## 1. 開工前必讀規則

每次使用者輸入：

```text
繼續開工
```

AI 必須先執行本文件的「模型開工檢查」。

AI 必須先回報：

```text
任務等級：
目前模型是否適合：
目前 reasoning 是否適合：
是否允許實作：
是否只能做 stub / interface / TODO：
是否需要使用者切換模型或調高 reasoning：
```

如果模型或 reasoning 不適合，AI 必須停止，不得硬做。

---

## 2. 任務等級定義

### Level A：低風險，可由 GPT-5.4-mini medium 執行

適合：

```text
前端頁面
React component
路由
表格
表單
mock service
API hook
CRUD 骨架
個人中心
首頁
市場列表
後台查詢頁
通知中心
文件整理
```

允許：

```text
可以直接實作。
可以新增 React 頁面與元件。
可以新增 mock data。
可以新增低風險 service。
可以跑 lint、typecheck、build。
```

建議：

```text
Model: GPT-5.4-mini
Reasoning: medium
```

---

### Level B：中風險，建議 GPT-5.4-mini high

適合：

```text
資料模型
service interface
API route skeleton
帳戶劃轉骨架
充值提現狀態流
Open API 簽名骨架
API key 管理
風控規則骨架
對帳 job skeleton
Java backend skeleton
```

允許：

```text
可以做 schema。
可以做 interface。
可以做 route。
可以做 service stub。
可以做狀態流。
可以寫 TODO。
```

禁止：

```text
不得完成真實資產扣帳。
不得完成真實成交結算。
不得完成真實強平。
不得完成真實保證金公式。
不得完成真實槓桿風險率。
不得處理真實私鑰或鏈上出帳。
```

建議：

```text
Model: GPT-5.4-mini
Reasoning: high
```

如果目前是 medium，也可以做，但只能做：

```text
schema
interface
route
stub
TODO
Java skeleton
```

---

### Level C：高風險，需要大模型 high 先設計或審查

適合：

```text
資產帳本真實邏輯
現貨成交結算
撮合一致性
合約倉位
保證金
PnL
標記價格
強平
保險基金
槓桿風險率
資金費率
對帳修復策略
MatchingEngineClient 接入層設計
C++ Core event schema
```

允許：

```text
只能做設計草案。
只能做 interface。
只能做 stub。
只能做 TODO。
只能做測試案例。
只能列風險清單。
```

禁止：

```text
不得自行完成生產級核心交易邏輯。
不得宣稱可以上線。
不得繞過人工審查。
不得把未審查公式寫入正式執行流程。
```

建議：

```text
Model: stronger model
Reasoning: high
```

如果目前是 GPT-5.4-mini，不管 medium 或 high，都只能做骨架與 TODO。

---

### Level D：極高風險，需要大模型 high + 人工審查

適合：

```text
私鑰管理
真實錢包出帳
提現放行
用戶資產修正
後台人工調帳
合約強平正式邏輯
槓桿壞帳處理
API withdraw 權限
做市商資金權限
正式上線前安全審查
任何會直接改資產或 ledger 的實作
任何 C++ Core production 級撮合、PnL、保證金、強平、槓桿風險率實作
任何 mock matching / mock order book / mock trade / mock settlement 被當成正式流程
```

允許：

```text
只能列審查清單。
只能列風險。
只能列必改項。
只能列測試項。
必須等待使用者確認。
```

禁止：

```text
不得修改生產邏輯。
不得直接實作。
不得自動批准。
不得自動修帳。
不得啟用 withdraw 權限。
```

建議：

```text
Model: stronger model
Reasoning: high
Human review: required
```

---

## 3. 每次開工前自檢流程

AI 每次開工前必須依序回答：

```text
1. 我現在要執行的 Phase 是什麼？
2. 我現在要執行的 Task 是什麼？
3. 此任務對應哪些文件？
4. 此任務屬於 Level A / B / C / D？
5. 目前模型是否適合？
6. 目前 reasoning 是否適合？
7. 我本次允許實作到什麼程度？
8. 我本次禁止碰哪些內容？
9. 是否需要先停下要求使用者換模型或調 reasoning？
10. 若是後端任務，是否已確認 Java 21 + Spring Boot 3 + `server/`？
```

---

## 4. 模型適配矩陣

| 任務等級 | mini medium | mini high | 大模型 high | 人工審查 |
|---|---:|---:|---:|---:|
| Level A | 可直接做 | 可直接做 | 可直接做 | 不必 |
| Level B | 只做骨架 | 可做骨架與狀態流 | 可做設計與審查 | 視情況 |
| Level C | 不可做核心，只能 stub | 不可做核心，只能 stub | 可做核心設計 | 必須審查 |
| Level D | 不可做 | 不可做 | 只可審查 | 必須 |

---

## 5. 如果模型不符合要求

AI 必須停止並回覆：

```text
目前任務屬於 Level C / Level D。
目前模型或 reasoning 不適合直接實作。
我不會繼續修改核心邏輯。

建議：
1. 切換到 stronger model。
2. Reasoning 設為 high。
3. 或允許我只建立 interface / stub / TODO。
```

---

## 6. 允許降級執行的方式

如果使用者仍希望低成本繼續，AI 只能做降級任務：

```text
建立資料夾
建立型別 / interface
建立 route skeleton
建立 service stub
建立 TODO
建立測試案例
建立審查 checklist
```

必須在程式碼或文件中標記：

```text
TODO: requires high-reasoning review before production use
```

---

## 7. 高風險關鍵詞偵測

如果任務或文件包含以下關鍵詞，AI 必須至少判斷為 Level B；若涉及正式邏輯，必須提升到 Level C 或 D。

### 至少 Level B

```text
ledger
wallet
withdraw
deposit
api key
signature
rate limit
risk
reconciliation
market maker
account transfer
```

### 至少 Level C

```text
matching
settlement
position
PnL
margin
funding rate
mark price
index price
liquidation
insurance fund
risk ratio
borrow
repay
interest
```

### Level D

```text
private key
hot wallet
cold wallet
real withdraw
manual adjustment
production launch
asset correction
bad debt
API withdraw permission
```

---

## 8. 開工前回報模板

每次開始前，AI 必須先輸出：

```text
模型開工檢查：

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
- 可以繼續 / 不可以繼續 / 只能做 stub
```

---

## 9. 完成後回報模板

每次完成後，AI 必須輸出：

```text
完成摘要：
- ...

模型分級確認：
- 本任務原判定：
- 實際執行範圍：
- 是否觸及高風險邏輯：

修改檔案：
- ...

新增檔案：
- ...

測試方式：
- ...

TODO：
- ...

風險：
- ...

是否需要人工審查：
- 是 / 否
```

---

## 10. 絕對禁止

無論任何模型，AI 都不得在未審查前做以下事情：

```text
直接修改用戶資產餘額
繞過 ledger service
實作真實錢包私鑰簽名
實作真實提現放行
開啟 API withdraw 權限
實作未審查的合約強平公式
實作未審查的保證金公式
實作未審查的 PnL 公式
實作未審查的槓桿風險率
自動修帳
宣稱可正式上線
```
