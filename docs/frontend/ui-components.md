# 管理後台 UI 元件與樣式盤點

2026-09-17 盤點；設計政策以 [LumiX Institutional Blue](../governance/lumix-institutional-blue.md) 為準。此文件只提供實作路由，不建立第二套視覺規範。檔案可能隨任務改動，施工前需重新搜尋確認。

## 現況

- `web/package.json`：React 19、TypeScript、Vite、React Router；目前未引入 Tailwind、Radix、Headless UI 或 icon library。
- `web/src/styles/global.css`：集中式 CSS，使用一般 class 與類 BEM 命名；已有少量 CSS variables，但多數顏色／spacing／字級仍為字面值，無完整獨立 theme token 模組。
- `web/src/admin.tsx`、`web/src/admin/AdminApp.tsx`、`web/src/admin/routes/AdminRouter.tsx`：後台入口與路由；服務隔離規則見根目錄 `AI_AGENT.md`。
- `.github/` 目前不存在；不可聲稱已存在 UI CI、PR template 或視覺回歸 gate，也不為文件任務新增空的 CI 框架。

## 可重用來源

2026-09-17 實作更新：`web/src/admin/components/AdminPageHero.tsx` 與同目錄 `AdminPageHero.css` 已提供共用 Hero；資產調整為第一個 consumer，使用者頁保留既有實作。Props 包含 title、description、icon、chips（id／label／icon）、illustration、slogan、supportingText，皆為呈現資料，不包含 mutation。容器低於 800px 隱藏右側裝飾，低於 480px 縮小 icon 與標題。資產圖像已移至 `features/assets/AssetAdjustmentArtwork.tsx`，SVG defs 以 `useId` 隔離。

| 目的 | 真實來源 | 使用限制 |
| --- | --- | --- |
| 卡片 | `web/src/components/base/Card.tsx` | 支援 title、children、className；先延伸已有結構 |
| 狀態 badge | `web/src/components/base/Badge.tsx` | neutral／success／warning／danger；功能 chips 不可假裝資料狀態 |
| 載入、空、錯誤 | `web/src/components/base/State.tsx` | 保留狀態語意及真實來源 |
| 確認對話框 | `web/src/components/base/ConfirmDialog.tsx` | 保留既有流程；重用前檢查 focus、鍵盤及 ARIA，不視為已通過無障礙驗收 |
| 基礎頁頭 | `web/src/components/layout/PageHeader.tsx` | 不等同完整 dashboard Hero |
| 基礎匯出 | `web/src/components/index.ts` | 先查 export，未匯出的元件按既有路徑引用 |
| Admin 外框 | `web/src/admin/layout/AdminLayout.tsx`、`AdminHeader.tsx`、`AdminTopNav.tsx` | 局部頁面美化預設不修改 |
| 使用者 Hero | `web/src/admin/features/users/AdminUsersPage.tsx`、`.admin-users-hero*` | inline markup／SVG 及 CSS，並非共用 `PageHero` |
| 資產調整 Hero | `web/src/admin/components/AdminPageHero.tsx`、`web/src/admin/features/assets/AssetAdjustmentArtwork.tsx` | 共用結構與 feature 圖像分離；form state 留在 `GovernedAirdropForm` |
| 表單 control | `global.css` 的 `.field`、`.input`、`.primary-button`、`.secondary-button` | 目前主要是原生 React 元素配 class，未有通用 Input／Button／Textarea TSX 元件 |
| 深色 dropdown | 同上資產表單的局部 `FormSelect` 與 `.admin-form-select*` | 外觀參考；抽取前驗證完整鍵盤操作，勿直接當成熟共享元件 |
| 翻譯 | `web/src/i18n/index.ts`、`web/src/i18n/dictionaries/zh-TW.ts`、`en-US.ts` | 沿用 `useI18n`／`t`；新增可見文案需同步語系 |

## 共用化時機

其他頁面需要相同 Hero 時優先採用現有 `AdminPageHero`，先檢查相依與標題階層；它目前使用 h2，頁面主標題仍由 page entry 負責。Dropdown 尚未抽成共用元件，仍須在抽取前檢查行為與可及性。

只抽取範圍內可驗證的共用樣式與行為，保持各頁語意、enum、API、權限及 reset defaults；不把資產 API、假 options 或 activity-specific 文案帶入共用 Hero。新的共享元件依現有目錄習慣落在 `web/src/components/` 或僅後台使用的 `web/src/admin/components/`，並更新本表。

同一視覺角色的顏色及尺寸使用 [語意 token 契約](ui-tokens.md)，不得把整頁 class 換名複製到另一頁。全站 CSS selector 的修改必須檢查前後台兩個 surface；局部 selector 不能以高 specificity 累加規則掩蓋衝突。
