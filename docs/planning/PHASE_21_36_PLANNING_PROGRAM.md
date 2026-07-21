# Phase 21–36 規劃計畫

## 狀態與目的

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

本文件是 Phase 21–36 的施工順序、風險門檻與任務草案索引。它不是 runtime approval、schema approval、production launch approval 或任何人類簽核紀錄。Phase 21 已有完整 task cards；Phase 22–24 有詳細 task drafts；Phase 25–28 有中階 task breakdown；Phase 29–36 有 phase charter 與高層任務。

## 全域施工規則

- runtime 只能按 phase 序號施工，且每張 task card 都要先經人類明確批准；本計畫不授權任何卡實作。
- 任一 phase 的 review 未通過，不得開始其後續 phase runtime；可進行的只有文件規劃與 review preparation。
- 不得以 P16–P20 sandbox foundation 推定正式 matching、fill、position、balance、reservation、ledger、settlement、fee 或 reconciliation runtime 已完成。
- 價格、數量、金額與費用禁止 binary floating point；資產異動一律可稽核、可重放、可對帳。
- production-ready 只能由 P36 在所有 `PRODUCTION_READINESS_GATES.md` 條件通過後，取得人類明確簽核時宣稱。

## 施工與能力依賴

```text
嚴格施工順序（不可跳階）
P21 -> P22 -> P23 -> P24 -> P25 -> P26 -> P27 -> P28
    -> P29 -> P30 -> P31 -> P32 -> P33 -> P34 -> P35 -> P36

能力依賴（不代表可平行實作）
P21 市場資料 ---------> P26 風控 -----> P27 管理 -----> P28 稽核
P22 鏈上監聽 ---------> P23 入金入帳 -----+
P24 提款請求 ---------> P25 簽章/廣播 ----+
P26/P27/P28 ------------------------------> P29 API、P30 UX
P23/P25/P29/P30 --------------------------> P31–P36 上線 gates
```

## 風險門檻

| 類別 | 觸發 phase | 不可省略的門檻 |
| --- | --- | --- |
| 行情完整性 | P21 | sequence/gap/stale/resync、精度、source time、可決定性重放 |
| 鏈上資料 | P22–P23 | chain identity、finality/reorg、idempotency、入帳與 reversal 可稽核 |
| 提款與資金 | P24–P25 | 權限分離、hold/release/capture、approval、簽章隔離、broadcast/reconciliation |
| 風控與管理 | P26–P28 | fail-closed limits、人工覆核、admin audit、不可竄改 evidence、資料最小化 |
| 公開介面 | P29–P30 | authN/authZ、rate limit、idempotency、精度/health UX、無 mock production claim |
| 營運與上線 | P31–P36 | metrics/logs/traces/alerts、備份/復原演練、security、容量、runbook、human sign-off |

所有含資金、ledger、reservation、withdrawal、risk、admin、security 或對外傳輸的 runtime task 都是 `HUMAN_REVIEW_REQUIRED`。風險評估不等於批准；若設計需要新增 migration、provider、secret、公開 endpoint 或權限，必須拆成獨立 task card。

## Phase 索引

| Phase | 規劃層級 | 核心目標 | runtime 狀態 |
| --- | --- | --- | --- |
| P21 | 完整 task cards | provider-neutral 唯讀行情資料 | not started |
| P22 | 詳細 task draft | 入金地址、鏈上觀測、reorg 邊界 | not started |
| P23 | 詳細 task draft | confirmation、credit decision、reconciliation | not started |
| P24 | 詳細 task draft | idempotent 提款請求與審核佇列 | not started |
| P25 | 中階分解 | approval、signing、broadcast、對帳 | not started |
| P26 | 中階分解 | 風控限制與市場保護 | not started |
| P27 | 中階分解 | 最小權限管理與受控操作 | not started |
| P28 | 中階分解 | 稽核、合規與證據匯出 | not started |
| P29 | Phase charter | 公開/私有 API 安全與可靠性 | not started |
| P30 | Phase charter | 正式交易 UX 與可信資料呈現 | not started |
| P31 | Phase charter | 可觀測性、告警與事件處置 | not started |
| P32 | Phase charter | 備份、復原與 replay 演練 | not started |
| P33 | Phase charter | 安全強化與安全證據 | not started |
| P34 | Phase charter | 負載、浸泡與混沌驗證 | not started |
| P35 | Phase charter | 營運、支援與商業就緒 | not started |
| P36 | Phase charter | 全面 launch gate 與人類簽核 | not started |

## 各波次停止條件

```text
P21：任何資料不完整、stale 或 gap 不得宣稱行情正常。
P22–P25：任何 finality、idempotency、權限、簽章或對帳契約未獲核准，即停止資金路徑施工。
P26–P30：任何風控 bypass、admin 越權、audit 缺口、API/UX 將 mock 或 stale 資料當正式，即停止。
P31–P36：任何 recovery、security、capacity、runbook、商業/合規證據或 human sign-off 缺失，即不得宣稱可上線。
```

## Review 與路由

- Phase 21：`docs/phases/PHASE_21_MARKET_DATA/README.md`
- Phase 22–36：各 phase README 內的 planning draft / charter。
- 全域審核：[PHASE_21_36_PLANNING_REVIEW.md](PHASE_21_36_PLANNING_REVIEW.md)
- 權威上線門檻：`docs/governance/PRODUCTION_READINESS_GATES.md`
