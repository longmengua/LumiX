# LumiX UI 長期治理與實作同步文件

> 此文件是 LumiX 管理後台 UI 的 living document。它記錄已落地的真實程式結構、治理決策、可重用元件、視覺缺口與同步責任；供 Codex、其他 AI、設計師與工程師共同使用。視覺政策以 [LumiX Institutional Blue](../governance/lumix-institutional-blue.md) 為準；本文不以理想規格覆蓋程式現況。

## 文件中繼資料

| 項目 | 值 |
| --- | --- |
| 最後更新 | 2026-09-18 |
| Repository revision | `7bf47d9` 之後的工作區：資產操作表單、平台內部轉帳與 UUID 掃碼，尚未建立新 revision |
| 前端框架 | React 19.1 + TypeScript 5.8 + Vite 6.3 |
| Router | React Router DOM 7.6，後台 `BrowserRouter basename="/admin"` |
| Styling | 集中式 CSS：`web/src/styles/global.css`；共用 Hero 額外使用 `AdminPageHero.css`；沒有 Tailwind、CSS Modules、SCSS 或 styled-components |
| Icon system | 頁面功能 icon 使用 inline React SVG；`qrcode.react` 僅用於真實 UUID 的 SVG QR Code |
| 前端根目錄 | `web/` |
| UI Governance Version | `1.13`（working tree snapshot） |

## 1. 設計語言：LumiX Institutional Blue

定位為 Premium Enterprise SaaS、Institutional Fintech 與 Digital Asset Infrastructure。介面應傳達安全、精準、透明、受治理、可追溯、專業與克制的未來感。

視覺基礎：深 navy／midnight blue、克制的 blue-indigo gradients、低強度半透明表面、細 cool-blue border、soft inner glow、controlled rim lighting、清楚的資訊層級與低雜訊裝飾圖形。

禁止：Cyberpunk、Gaming UI、過量 neon、彩虹漸層、過量 glow、視覺雜訊、與功能無關的 3D 裝飾、寫實人物、Anime、Mascot、Meme。

完整視覺規則、Golden Reference 使用方式與驗證流程見 [LumiX Institutional Blue](../governance/lumix-institutional-blue.md)。本文件的優先責任是讓外部協作者知道「目前 code 已經是什麼」。

## 2. UX 優先序

1. 可用性（Usability）
2. 資訊層級（Information hierarchy）
3. 一致性（Consistency）
4. 可及性（Accessibility）
5. 視覺品質（Visual quality）
6. 裝飾效果（Decorative effects）

任何 gradient、glow、插圖或動態都不得遮蔽操作、降低對比、破壞鍵盤操作或改變業務行為。

## 3. Repository UI Architecture

| 領域 | 真實路徑／實作 |
| --- | --- |
| Admin app entry | `web/src/admin.tsx` → `web/src/admin/AdminApp.tsx` |
| Admin routes | `web/src/admin/routes/AdminRouter.tsx` |
| Admin shell | `web/src/admin/layout/AdminLayout.tsx`、`AdminHeader.tsx`、`AdminTopNav.tsx` |
| Feature pages | `web/src/admin/features/` |
| Admin-only shared components | `web/src/admin/components/` |
| Cross-surface shared components | `web/src/components/base/`、`web/src/components/layout/` |
| Global styles and root variables | `web/src/styles/global.css` |
| i18n dictionary | `web/src/i18n/dictionaries/zh-TW.ts`、`en-US.ts` |
| Feature artwork | 資產調整 Hero raster artwork 位於 `web/src/assets/hero/`；feature component 位於 `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx` |
| Static asset root | `web/public/`；可由 Vite import 的 Hero raster assets 位於 `web/src/assets/hero/` |

後台服務入口以 `/admin` 為 basename；`AdminRouter` 內的 `assets/*` 與 `users/*` 因此分別對應瀏覽器路徑 `/admin/assets/...` 與 `/admin/users/...`。

## 4. Canonical Design Tokens

### 已落地、可供後台新 Hero／資產表單使用

來源：`web/src/styles/global.css` 的 `.admin-layout`。這些不是 `:root` 全站 token；前台不應假設可用。

| 分類 | 真實 token | 現值／用途 |
| --- | --- | --- |
| Surface | `--color-bg-surface` | `#07172f`，深色 surface |
| Control surface | `--color-bg-control` | `rgba(3, 16, 40, .65)`，input／chip 底色 |
| Subtle border | `--color-border-subtle` | `rgba(100, 153, 242, .25)` |
| Focus border | `--color-border-focus` | `rgba(87, 155, 255, .6)` |
| Primary text | `--color-text-primary` | `#edf5ff` |
| Secondary text | `--color-text-secondary` | `#adbedc` |
| Muted text | `--color-text-muted` | `#8fa5c8`，Hero value supporting text |
| Primary accent | `--color-accent-primary` | `#73b1ff` |
| Secondary accent | `--color-accent-secondary` | `#8579f4` |
| Hero surface | `--gradient-hero` | radial blue glow + navy linear gradient |
| Surface shadow | `--shadow-surface` | `0 14px 36px rgba(0, 6, 22, .18)` |
| Hero shadow / glow | `--shadow-hero` | deep surface shadow + restrained blue outer glow |
| Hero rim | `--color-hero-rim` | `rgba(103, 165, 255, .52)`，Hero thin cool-blue border |
| Hero inner highlight | `--color-hero-highlight` | `rgba(207, 229, 255, .16)`，頂部 edge lighting |
| Hero value text | `--color-hero-value` | `#d8e9ff`，右側 value proposition |
| Card / hero radius | `--radius-card` | `1.25rem`（20px） |
| Control radius | `--radius-control` | `.7rem`（約 11px） |
| Pill radius | `--radius-pill` | `999px` |
| Hero title | `--font-size-hero-title` | `2.5rem`（40px） |
| Body | `--font-size-body` | `.9375rem`（15px） |
| Helper | `--font-size-helper` | `.8125rem`（13px） |
| Spacing | `--space-2/3/4/5/6/8` | `.5/.75/1/1.25/1.5/2rem`（8/12/16/20/24/32px） |

### Root layout variables

來源同為 `global.css` 的 `:root`：`--content-max: 90rem`、`--content-gutter: clamp(1rem, 2vw, 2rem)`、`--section-gap: clamp(1rem, 2vw, 1.5rem)`、`--panel-min: min(100%, 18rem)`。字型為 `Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`。

### 尚未成為 canonical token 的語意色

目前沒有 `--color-state-success`、`--color-state-warning`、`--color-state-danger`、`--font-size-page-title`、`--font-size-section-title`、`--font-size-metadata`、`--glow-accent` 或一般性 `--radius-small`。成功／警告／危險色仍散落在 component CSS（例如 `Badge`、表單錯誤與使用者狀態）。新增前先在既有 token 中搜尋；不要在單頁私建同義 token。

## 5. Canonical Components

| Component | Path | Purpose / important props | Styling | Status |
| --- | --- | --- | --- | --- |
| `AdminPageHero` | `web/src/admin/components/AdminPageHero.tsx` | 管理頁 Hero；`icon`、`title`、`description`、`chips`、`illustration`、`slogan`、`supportingText`；只組合呈現，不含 mutation 或資料請求 | `web/src/admin/components/AdminPageHero.css` | Canonical（資產調整、使用者） |
| `AssetAdjustmentHeroIcon` | `web/src/admin/features/assets/AssetAdjustmentHeroIcon.tsx` | 84×84 內嵌 SVG 紫藍 icon tile | feature SVG + `global.css` 的 `--asset-adjustment-icon-*` aliases | Feature-specific |
| `AssetAdjustmentArtwork` | `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx` | supplied V2 raster Hero artwork；`picture` 依 source variant 載入透明 WebP | `AdminPageHero.css` control box；assets 位於 `web/src/assets/hero/` | Feature-specific |
| `PageHeader` | `web/src/components/layout/PageHeader.tsx` | 外層頁名、description、actions；不是 dashboard Hero | `global.css` | Canonical base component |
| `Card` | `web/src/components/base/Card.tsx` | 通用 card，`title`、`children`、`className` | `global.css` | Canonical base component |
| `Badge` | `web/src/components/base/Badge.tsx` | neutral／success／warning／danger 狀態標籤 | `global.css` | Canonical base component |
| `CopyButton` | `web/src/components/base/CopyButton.tsx` | 複製 UUID、email 等識別值；`value`、`label`、`copiedLabel`；含 Clipboard API 與受限環境 fallback | `.copy-button` in `global.css` | Canonical base component（管理端使用者列表、客戶端個人中心） |
| `LoadingState`／`EmptyState`／`ErrorState` | `web/src/components/base/State.tsx` | 真實資料 loading／empty／error 呈現 | `global.css` | Canonical base component |
| `FormSelect` | `web/src/admin/features/assets/GovernedAirdropForm.tsx` | 資產調整局部深色 listbox，props 為 `ariaLabel`、`options`、`value`、`onChange`、`compact`、`disabled` | `.admin-form-select*` in `global.css` | Local / not yet extracted |
| `AssetAdjustmentAuditPanel` | `web/src/admin/features/assets/AssetAdjustmentAuditPanel.tsx` | 讀取既有資產沖銷／空投調整的對賬結果；loading、empty 與 error 均重用共享 State 元件 | `.admin-asset-audit*` in `global.css` | Feature-specific readonly panel |
| `AssetFundingPage` | `web/src/pages/assets/AssetFundingPage.tsx` | `/assets/deposit`、`/assets/withdraw` 共用的方式入口；平台內部轉入顯示 UUID QR、轉出提交真實命令 | `.asset-funding-*` in `global.css` | Feature-specific |
| `AssetSymbolSelect` | `web/src/features/assets/AssetSymbolSelect.tsx` | 現貨資產下拉；呼叫端提供真實 asset options、value 與 onChange，不可建立 fallback 幣種 | `.admin-form-select*` in `global.css` | Canonical asset control（帳戶劃轉、平台內部轉出） |

目前沒有 shared React `Input`、`Select`、`Textarea`、`Button` 或第三方 icon component。一般 control 是原生元素加 `.input`、`.primary-button`、`.secondary-button` class。使用者 Hero 也不是 `AdminPageHero` consumer。

## 6. Canonical Hero Pattern

完整 Hero 範圍的 desktop 結構：

```text
[功能 icon] [標題 / 描述 / 0–3 contextual chips] [功能插圖] [價值主張 / 輔助說明]
```

目前 `AdminPageHero` geometry（`AdminPageHero.css`）：

| 項目 | 實際設定 |
| --- | --- |
| Layout | CSS Grid；以既有 `identity`／`visual` wrapper 的 `display: contents` 映射 icon、copy、artwork、value 四欄 |
| Minimum height | desktop 220px；desktop visual validation result 約 222px |
| Padding | desktop horizontal `32px`；container ≤1024px 時 `22px 26px` |
| Radius | `--radius-card`（20px） |
| Icon box | `84×84px`；容器 ≤480px 改為 `64×64px` |
| Icon-to-copy gap | `--space-6`（24px） |
| Title | 40px / weight 740 / line-height 1.16；≤480px 為 28px |
| Description | 15px / line-height 1.6 |
| Chips | 13px；padding 8px 12px；gap 8px；pill radius |
| Visual region | desktop artwork column `minmax(360px, 400px)` + value `minmax(180px, 210px)`；column gap 24px |
| Illustration box | desktop `390px × 220px`；container ≤1180px 時 `300px × 169px`，≤1024px 時 `230px × 129px` |

此 pattern 是呈現規範，不授權自行為任何頁面加入 chips、改文案或新增 Hero；需有明確任務範圍與真實產品語意。

## 7. Illustration Governance

Hero 插圖優先採用 inline SVG + CSS/SVG gradients，避免外部圖片 URL、base64 與不必要 dependency；已有批准的 raster handoff 時，可由 Vite import 的透明 WebP 取代 SVG。可用 coins、users、shields、wallet、documents、data blocks、adjustment arrows、orbit lines 與少量 floating spheres，但必須與頁面功能語意相關。

- 裝飾插圖必須 `aria-hidden`，不承載必要 business information，不可攔截操作。
- 使用 filled shapes、gradient、受控 shadow、有限 spotlight 建立 pseudo-3D；避免 outline-only、過度 blur 或 neon。
- 可重複掛載 SVG 的 `<defs>` 使用 `useId()` 產生唯一 ID，避免 gradient/filter collision。
- Raster assets 必須具明確 handoff、透明背景、`alt=""`／`aria-hidden` 與 responsive source 策略；不得使用外部 URL 或 base64。

| Illustration | Path | Implementation | Status |
| --- | --- | --- | --- |
| `AssetAdjustmentArtwork` | `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx` | `<picture>`：desktop `asset-adjustment-hero-illustration.webp`、viewport ≤1440px 的 narrow/tablet `asset-adjustment-hero-illustration-md.webp`、≤760px small fallback `asset-adjustment-hero-illustration-sm.webp`，均在 `web/src/assets/hero/` | Integrated |
| 使用者 Hero decoration | `web/src/admin/features/users/AdminUsersPage.tsx` + `.admin-users-hero__orbs*` in `global.css` | CSS gradients + pseudo elements + spans；不是獨立 component | Legacy page-specific |

沒有名為 `UserHeroIllustration` 的 component；外部協作者不得假設其存在。

## 8. Page Registry

### Asset Management / Adjustment

| 項目 | 真實狀態 |
| --- | --- |
| Route | `/admin/assets/adjustments` |
| Route entry | `web/src/admin/routes/AdminRouter.tsx` → `assets/*` |
| Page | `web/src/admin/features/assets/AdminAssetsPage.tsx` (`AdminAssetsPage`) |
| Adjustment panel | `GovernedAirdropForm` in `web/src/admin/features/assets/GovernedAirdropForm.tsx` |
| Hero | `AdminPageHero` |
| Illustration | `AssetAdjustmentHeroIcon`、`AssetAdjustmentArtwork`（透明 WebP raster artwork） |
| Shared components used | `PageHeader`、`Card`、`AdminPageHero`；native controls with global classes |
| Current status | Implemented；supplied artwork/CSS handoff 已於 `974f4a5` 整合 |
| Business behavior | Preserved: user ID / activity / signed amount / configured asset / required note validation, reset, loading / double-submit guard, server API and result/error feedback |

Hero content is i18n-driven (`zh-TW.ts`) and currently resolves to:

```text
Title: 資產調整
Description: 執行資產沖銷、補正與治理調整，完整保留審核與服務軌跡。
Chips: 沖銷作業 / 補正處理 / 審核留痕
Value: 精準・可溯・合規
Supporting: 建構更透明的數位資產治理
```

### Asset Management / Reconciliation & P&L

| 項目 | 真實狀態 |
| --- | --- |
| Route | `/admin/assets/audit` |
| Page host | `web/src/admin/features/assets/AdminAssetsPage.tsx` (`AdminAssetsPage`) |
| Panel | `AssetAdjustmentAuditPanel` in `web/src/admin/features/assets/AssetAdjustmentAuditPanel.tsx` |
| Read API | `GET /api/admin/v1/assets/audit/adjustments` |
| Server boundary | `server/src/main/java/com/lumix/admin/asset/AdminAssetAdjustmentAuditController.java` → `AdminAssetAdjustmentAuditService` → `JdbcAdminAssetAdjustmentAuditQueryRepository` |
| Current status | Implemented：只讀資產沖銷／空投調整的證據交叉核對 |
| Data source | `audit_logs`、`ledger_journals`、`ledger_entries`、目標帳戶與使用者；不使用 projection 或 browser 推算作為對賬真相 |
| Business behavior | Server-side super-admin gate；固定最多 100 筆；雙分錄筆數、方向、同資產、借貸平衡與目標帳戶分錄不符合時回傳 `EXCEPTION`，只能人工處理 |

手續費收入、營收、虧損與 P&L 彙總尚未建立可用帳務來源。這個頁面不顯示預估、零值、mock 或假成功數字；待真實 fee journal／收益歸屬 runtime 完成後，才可擴充對賬範圍。

### Client Asset Funding

| 項目 | 真實狀態 |
| --- | --- |
| Routes | `/assets/deposit`、`/assets/withdraw` |
| Page | `web/src/pages/assets/AssetFundingPage.tsx` (`AssetFundingPage`) |
| Shared components used | `AdminPageHero`、`AssetSectionNav`、`Card`、`CopyButton`、`ErrorState`；控制項重用 `.input`、`.admin-form-select*`、`.primary-button` |
| Platform internal receive | `/assets/deposit` 的「平台內部轉入」以 `fetchAccountProfile()` 讀取登入者 `userId`，用 `QRCodeSVG` 編碼該 UUID，並提供 `CopyButton`；不產生鏈上地址或資產異動 |
| Platform internal send | `/assets/withdraw` 的「平台內部轉出」以 `useAssetProjectionSnapshot()` 取得 ACTIVE SPOT 幣種，向 `POST /api/v1/assets/internal-transfers` 送出收款 UUID、資產與正數量；收款 UUID 欄位可開啟瀏覽器原生 Camera API + `BarcodeDetector` 掃描真實 UUID QR Code 並自動帶入；不接受 browser account ID |
| Account transfer | `/assets/transfer` 的帳戶劃轉維持 `POST /api/v1/assets/transfers` payload；表單改為來源帳戶、目標帳戶、數量＋資產三列。資產選項由所選來源帳戶的真實 ACTIVE projection 提供，並在帳戶選單下顯示該幣種的目前可用／目前持有；沒有資料一律顯示 `—`，不補零值 |
| Other methods | 鏈上、OTC、銀行僅保留受控「即將開放」說明；沒有 provider、地址、外部結算或 fake success |
| Current status | Platform internal receive/send implemented；外部通道 unavailable |

### Users

| 項目 | 真實狀態 |
| --- | --- |
| Route | `/admin/users/list` |
| Route entry | `web/src/admin/routes/AdminRouter.tsx` → `users/*` → `AdminUsersWorkspacePage` |
| Workspace | `web/src/admin/pages/AdminNavWorkspaces.tsx` |
| Primary page | `web/src/admin/features/users/AdminUsersPage.tsx` (`AdminUsersPage`) |
| Hero | `AdminPageHero` |
| Illustration | `UsersHeroArtwork` local CSS orb decoration，透過 shared Hero illustration slot 掛載 |
| Shared components used | `AdminPageHero`、`EmptyState`、`InlineErrorState`、`ConfirmDialog`；原生 form controls；workspace uses `PageHeader` / `Card` |
| Current status | Implemented / canonical Hero consumer |
| Business behavior | Preserved: query, date filters, pagination, account / asset / history reads, loading and error states；列表不再 inline detail，改由右側管理抽屜呈現。登入／提幣凍結仍只經最高管理員保護的目標狀態命令，完成後依 server response 更新命中列 |

目前頁內 Hero content：

```text
Title: 使用者
Description: 管理平台帳號與登入活動
Value: 安全・透明・高效
Supporting: 建構更安全的數位資產未來
Chips: 未實作（不得因文件範例自行補上）
```

## 9. Implementation Snapshot

資產調整實際 DOM/component outline：

```text
AdminRouter
└─ AdminRequireAuth
   └─ AdminLayout
      ├─ AdminHeader
      └─ AdminAssetsPage
         ├─ PageHeader
         └─ admin-assets-workspace
            ├─ asset workspace sidebar tabs（用戶資產／資產沖銷／對賬審計與損益）
            └─ AdjustmentWorkspace
               └─ GovernedAirdropForm
                  └─ admin-airdrop-form
                     ├─ AdminPageHero
                     │  ├─ AssetAdjustmentHeroIcon
                     │  ├─ title / description / chips
                     │  ├─ AssetAdjustmentArtwork (`picture` / transparent WebP)
                     │  └─ slogan / supporting text
                     └─ adjustment form
```

資產工作台以 CSS Grid（`minmax(11rem, 14rem) minmax(0, 1fr)`）放置左側 tabs 與內容。Hero 使用 CSS Grid；表單主要欄位使用 two-column CSS Grid；其餘 form behavior 留在 `GovernedAirdropForm`。Hero 位於 `admin-airdrop-form` 的同一 outer surface 上，Hero 之後才是 form body。

資產對賬頁在相同 workspace sidebar 下改為：

```text
AdminAssetsPage
└─ admin-assets-workspace
   └─ AssetAdjustmentAuditPanel
      ├─ scope notice
      └─ immutable adjustment reconciliation table
         ├─ loading / empty / error state
         └─ verified or exception rows
```

這個面板不共用資產調整 Hero，也不提供修正 CTA；它是為既有流水提供核對可見性，而非第二個資產操作入口。

使用者頁實際 DOM/component outline：

```text
AdminUsersWorkspacePage
└─ AdminNavWorkspace
   ├─ PageHeader
   ├─ workspace sidebar tab
   └─ AdminUsersPage
      └─ admin-users-page
         ├─ AdminPageHero
         │  ├─ UsersIcon tile
         │  ├─ title / description
         │  ├─ UsersHeroArtwork (CSS orbs)
         │  └─ value text
         └─ users toolbar / scan-oriented list / pagination
      └─ UserManagementDrawer (portal-like fixed overlay)
         ├─ Overview / Assets / Security and restrictions / Records tabs
         └─ restriction ConfirmDialog
```

客戶端充值／提幣的實際 outline：

```text
ClientRouter
└─ RequireAuth
   └─ AssetFundingPage
      ├─ AdminPageHero（共享呈現 shell）
      ├─ AssetSectionNav
      └─ asset-funding-card
         ├─ 方法選擇（internal / chain / OTC / bank）
         └─ internal
            ├─ deposit: Account Profile → UUID QR Code + CopyButton
            └─ withdraw: Asset Projection → recipient UUID + amount + asset select → internal transfer API
```

## 10. Responsive Implementation

| Area | Desktop | Narrow desktop / tablet | Mobile |
| --- | --- | --- | --- |
| `AdminPageHero` | four-column Grid: icon / copy / feature artwork / value；asset uses 390px raster artwork, users uses CSS orb artwork | container ≤1024px: icon / copy / reduced artwork; value hidden | container ≤760px: icon / copy only; artwork and value hidden |
| Asset workspace | left grid sidebar 11–14rem | same until viewport 767px | viewport ≤767px: single-column workspace; tabs become flex-wrap |
| Adjustment form | user/type two columns; amount occupies first column | follows available container | viewport ≤767px: field grid becomes one column; footer/actions stack as defined in `global.css` |
| Users Hero | heading + right decoration | viewport ≤1100px hides decoration | viewport ≤767px: icon 3.8rem, title 1.7rem, reduced padding; workspace controls wrap |
| Client funding internal receive | copy + QR side-by-side | same surface and wrapping identifier | QR moves below copy; no horizontal overflow |
| Client funding internal send | recipient / amount-and-asset two-row form；UUID label has QR scan control | same single-column rhythm | same order; submit button becomes full width |
| Client account transfer | source / target / unified amount-and-asset three-row form | same single-column rhythm | amount and asset retain one control row；asset selector contracts without overflow |

`AdminPageHero` deliberately uses **container queries**, not viewport queries, because the fixed workspace sidebar and localization affect the available content width.

## 11. Critical Implementation Details and Protected Behavior

### Protected layout boundaries

Unless explicitly authorized, do not alter `AdminHeader` / top nav, `AdminLayout`, the asset workspace sidebar, route architecture, auth boundary, `PageHeader`, or workspace widths while refining a local Hero or form.

### Asset Adjustment protected behavior

- API functions are `createAdminAirdrop(form)` and `fetchAdminAirdropAssetOptions()` from `web/src/admin/api/adminAssetsApi.ts`; UI work must not change their endpoint, HTTP method, payload, enum values or result handling.
- Activity values are true request values: `REVERSAL` and `AIRDROP`; they are rendered from local options and must not be replaced by visual-only values.
- Asset options come from the admin API. The UI defaults to the first returned asset, does not use a fake fallback, and disables submission when assets are unavailable.
- Amount accepts the existing signed decimal pattern; no UI task may replace the existing controlled amount handling with JavaScript floating-point conversion.
- User ID, asset, amount and reason validations; 256-character note limit; reset default; loading / disabled submit; duplicate-submit guard; server result/error feedback are protected.
- The form is a privileged asset action: permission, immutable ledger, audit, idempotency and server-side decision remain outside the browser component.

### Asset adjustment reconciliation protected behavior

- `GET /api/admin/v1/assets/audit/adjustments` 是唯讀最高管理員 API；不得擴充為補帳、重放、修改餘額或修改 audit evidence 的 command。
- 對賬狀態只可由 server 比對既有 `audit_logs`、journal 與 ledger entries 產生；browser 不得自行把資料標示為完成。
- `EXCEPTION` 是人工調查訊號，不是前端自動修復、反沖、重送或資產異動授權。
- 現階段範圍只包含 `ADMIN_ASSET_ADJUSTMENT` 成功證據與 `ADJUSTMENT` journal。手續費收入、營收、虧損與 P&L 沒有真實資料來源前不得加入任何 placeholder 數字。

### Client platform internal transfer protected behavior

- QR Code 只可編碼已認證使用者的 UUID；不可把鏈上地址、私鑰、token 或付款 URL 放入 QR。
- 收款 UUID、asset symbol 與 amount 必須由 server 驗證；browser 不可傳入 source/destination account ID、帳本識別碼或任何可繞過 ownership 的欄位。
- 轉出幣種只來自真實 ACTIVE SPOT projection；資料 loading/error/empty 時必須停用送出，不能寫死幣種、餘額或成功狀態。
- `POST /api/v1/assets/internal-transfers` 是高風險受治理資產命令：idempotency、reservation、immutable ledger、projection rebuild、audit 與 fail-closed server boundary 不可因 UI 任務變更；`HUMAN_REVIEW_REQUIRED`。

### Users protected behavior

- Preserve name search, created / last-login date filters, reset, cursor pagination, expanded detail rows and independent reads for detail, accounts, assets and history.
- Preserve loading/error state semantics and existing authorization/API boundaries.
- `PUT /api/admin/v1/users/{userId}/restrictions/login` 與 `/withdrawal` 是最高管理員專用的受治理目標狀態命令；browser 只能傳送 `frozen`，不可指定 session、帳戶、餘額或稽核資料。
- 登入凍結必須撤銷既有 session；提幣凍結必須由平台內部對他人轉出 runtime fail closed。UI 不可用本地 optimistic state 取代後端回讀。
- 使用者清單只呈現帳戶狀態、限制摘要與「管理」入口。詳細資料、帳戶、資產、紀錄與高風險限制都只能在 `UserManagementDrawer` 內處理；未有後端 API 的轉帳／現貨／合約限制僅以 disabled capability row 呈現。
- `system:` 前綴是既有 system principal 規則。系統帳戶要顯示 SYSTEM／不適用，且不應提供限制操作；server-side command 也必須拒絕它。
- Do not turn Hero decoration, status color or table rows into invented operational data or unrelated CTAs.

## 12. Known Visual Gaps

### Asset Adjustment Hero V2 raster handoff — Integrated in working tree

| Field | Current | Target | Resolution |
| --- | --- | --- | --- |
| Area | Existing `AdminPageHero` now maps the handoff's four-zone grid, artwork box and value styling without changing its props | Supplied Asset Adjustment Hero handoff | Desktop screenshot at 1600px validates the full four-zone Hero; 1024px hides only value and 390px hides artwork/value without horizontal overflow |
| Area | Supplied transparent WebP artwork replaces the previous SVG and uses 390px desktop render width | `lumix_asset_hero_v2.zip` raster handoff | `picture` loads desktop/md/small variants from `web/src/assets/hero/`; CSS preserves aspect ratio with `object-fit: contain` and container-query hide behavior |

### Cross-page Hero consistency

| Field | Current | Target | Gap | Priority |
| --- | --- | --- | --- | --- |
| Area | Asset adjustment and users consume `AdminPageHero` | Shared LumiX visual language with semantics preserved | Resolved: both pages share Hero geometry, icon tile, typography hierarchy and container-query downgrade; feature artwork remains semantic and page-local | Resolved |
| Area | Some user and form styles still use local literals | Semantic token-led shared surfaces | First token set only covers new admin Hero/assets consumers; color/state/typography token migration is incomplete | Medium |

### Control system

| Field | Current | Target | Gap | Priority |
| --- | --- | --- | --- | --- |
| Area | Asset `FormSelect` is local to the form | Shared accessible select only after verified behavior | It is not a canonical shared component; Arrow Up roving focus and broader consumer testing are not yet established | Medium |

When a task closes one of these gaps, update or remove the row in the same change. This section is evidence of known state, not an unconditional backlog.

## 13. Reuse Policy

```text
reuse > extend > create
```

First locate an existing shared component and token. Extend a semantic prop API only when it preserves the current consumers. Create a new abstraction only for a repeated, verifiable pattern; two actual consumers is the normal threshold. Do not copy/paste Hero JSX or CSS into another feature, and do not over-abstract a one-off local requirement.

## 14. AI Working Protocol

### Before

1. Read this document, then its linked visual policy and component/token inventory as needed.
2. Run `git status --short`; preserve unrelated or user-owned changes.
3. Locate the route, page, shared components, variables, i18n source, responsive rules and protected behavior.
4. Check this document's Known Visual Gaps and the task's permitted/frozen areas.

### During

1. Preserve API contracts, data model, permission, validation, loading/reset/error behavior and page information architecture unless explicitly authorized.
2. Reuse canonical components and tokens before creating alternatives.
3. Apply LumiX Institutional Blue with the UX priority order above.
4. Do not add invented statistics, CTAs, mock data or page-private duplicate CSS.
5. 保持 SVG 或已批准的 raster artwork 為純裝飾性、可縮放／可替換並具 `aria-hidden` 或空 `alt`；功能 control 必須維持可及性。

### After

1. Verify the affected UI behavior and responsive states in proportion to the change.
2. Update the relevant Page Registry, Implementation Snapshot, Canonical Components/Tokens, Responsive Implementation and Known Visual Gaps.
3. Add a Decision Log entry only for material architectural or visual decisions.
4. Update Last Updated, Repository revision (when known), version and Change History.

## 15. Documentation Sync Rule

The following changes **must** update this file in the same change set: component path/API, Hero structure, design token, illustration, responsive behavior, canonical styling, page status, approved visual target or a known visual gap. A UI change without this synchronization is incomplete.

Do not record an uncommitted implementation as a released revision. If a working-tree snapshot is intentionally documented, mark it as uncommitted exactly as this initial entry does.

## 16. Decision Log

### 2026-09-17 — Separate visual policy from implementation snapshot

**Context:** `docs/governance/lumix-institutional-blue.md` already owns visual policy, while UI code evolves faster and needs a portable external-AI snapshot.

**Decision:** Keep visual policy in `docs/governance/`; use this `docs/ai/` document as the mandatory living implementation registry and synchronization ledger.

**Affected:** All future admin UI work.

**Reason:** Avoid two conflicting design specifications while ensuring external collaborators can understand the actual component and token state.

### 2026-09-17 — Asset adjustment is the first `AdminPageHero` consumer

**Context:** The asset adjustment Hero required a reusable presentation shell without coupling shared UI to a privileged mutation.

**Decision:** `AdminPageHero` accepts presentation slots only; feature artwork and form state remain in the assets feature.

**Affected:** `AdminPageHero`, `GovernedAirdropForm`, `AssetAdjustmentArtwork`.

**Reason:** Enables reuse without leaking asset API or form semantics into a shared component.

### 2026-09-17 — Refine the shared Hero surface without changing the asset command form

**Context:** The approved asset-adjustment reference showed the existing Hero lacked material separation and the illustration/value block had insufficient independent weight.

**Decision:** Extend only existing Hero semantic tokens, `AdminPageHero.css` and the existing asset SVG. Keep all text, layout ownership, form state and API behavior unchanged.

**Affected:** `AdminPageHero`, `AssetAdjustmentHeroIcon`, `AssetAdjustmentIllustration`.

**Reason:** The refinement strengthens the canonical Hero pattern while avoiding a second visual system or a page-level copy of shared CSS.

### 2026-09-17 — Integrate supplied Asset Adjustment Hero handoff through existing selectors

**Context:** The handoff supplies a complete asset artwork and a four-zone Hero layout, while the repository already has `AdminPageHero` wrappers and feature-level icon ownership.

**Decision:** Preserve the `AdminPageHero` API; map the handoff grid to existing selectors with `display: contents`, move the supplied SVG into the assets feature, and retain the existing feature icon component.

**Affected:** `web/src/admin/components/AdminPageHero.css`, `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx`, `web/src/admin/features/assets/AssetAdjustmentHeroIcon.tsx`.

**Reason:** Integrates the supplied visual source without duplicating Hero markup, CSS hierarchies or business form behavior.

### 2026-09-17 — Use approved V2 raster artwork through the existing Hero slot

**Context:** `lumix_asset_hero_v2.zip` supplies transparent WebP desktop, medium and small artwork variants with a materially larger approved composition.

**Decision:** Keep `AdminPageHero` and its container-query layout intact; replace only `AssetAdjustmentArtwork` internals with a decorative `picture`, using Vite-imported variants from `web/src/assets/hero/`.

**Affected:** `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx`, `web/src/admin/components/AdminPageHero.css`, `web/src/assets/hero/`.

**Reason:** Preserves the supplied raster artwork without reinterpreting it, while retaining the shared Hero API and responsive layout behavior.

### 2026-09-17 — Converge Users and Assets on the canonical Hero shell

**Context:** The users workspace retained a page-local Hero with different height, grid geometry and responsive rules, creating a visible shift when switching between Users and Assets.

**Decision:** Replace only the users Hero wrapper with `AdminPageHero`; retain users title, description, value copy and local orb artwork as semantic slots.

**Affected:** `web/src/admin/features/users/AdminUsersPage.tsx`, `web/src/styles/global.css`, `AdminPageHero` consumers.

**Reason:** Gives Users and Assets the same Hero surface, scale and downgrade behavior without coupling user read models to asset form behavior.

### 2026-09-17 — Product account surfaces are limited to spot and futures

**Context:** LumiX 已確定不提供獨立現貨槓桿／借貸產品；現貨與合約是唯一使用者帳戶類型。

**Decision:** 移除前端獨立槓桿 route、tab、legacy mock 與 copy；資產 API contract 僅接受 `SPOT`／`FUTURES`。合約商品所需的保證金、槓桿與風控語意仍保留，不能誤當成已移除的現貨借貸。

**Affected:** 資產帳戶 navigation、前台交易 route／mock types、i18n copy、資產 projection/history contract。

**Reason:** 降低借貸、利率、負債與清算風險，同時維持合約交易的必要風控邊界。

### 2026-09-18 — 平台內部轉入／轉出採真實 UUID 與資產命令邊界

**Context:** 充值與提幣方式入口需要支援 LumiX 使用者間的現貨資產移轉，不能以假地址、假 QR 或前端成功狀態代替。

**Decision:** 平台內部轉入顯示登入者真實 UUID 的 decorative QR Code 與複製控制項；平台內部轉出只從真實現貨餘額 projection 取得幣種，提交至受 session、idempotency、reservation、immutable ledger 與 audit 保護的 server command。

**Affected:** `web/src/pages/assets/AssetFundingPage.tsx`、`web/src/features/assets/assetProjectionApi.ts`、`server/src/main/java/com/lumix/account/runtime/PlatformInternalTransferService.java`。

**Reason:** 保持使用者可理解的收款方式，同時不讓 browser 指定 account ID、修改餘額或假裝資產已移動。

### 2026-09-18 — 使用者管理新增受治理的登入與提幣限制入口

**Context:** 管理人員需能在使用者清單中凍結登入或提幣，但 UI 不能成為單純前端狀態開關。

**Decision:** `AdminUsersPage` 以確認視窗呼叫受 server-side super-admin gate 保護的目標狀態 API；清單與詳情在命令完成後重新讀取後端狀態。登入凍結會撤銷 session；提幣凍結會阻擋平台內部對他人轉出。按鈕與限制摘要使用使用者可理解的「凍結／解除凍結」詞彙。

**Affected:** `web/src/admin/features/users/AdminUsersPage.tsx`、`web/src/admin/api/adminUsersApi.ts`、管理端使用者清單樣式與雙語字典。

**Reason:** 保留可追溯、可回讀的高風險操作邊界，並避免管理端顯示與實際執行結果脫節。

## 17. Change History

### v1.0 — 2026-09-17

- 建立 UI 長期治理與實作同步文件。
- 登錄已落地 token、`AdminPageHero`、資產調整 SVG artwork 與使用者頁 page-specific Hero。
- 建立資產調整與使用者頁 registry、受保護行為、responsive snapshot 與已知視覺缺口。

### v1.1 — 2026-09-17（working tree）

- 強化資產調整 Hero 的 layered surface、semantic Hero tokens、icon material depth、SVG illustration 與 value accent line。
- 以 1600px、1024px 與 390px 截圖確認 Hero geometry 與無 horizontal overflow。

### v1.2 — 2026-09-17（working tree）

- 整合 supplied `AssetAdjustmentArtwork` handoff 與四欄 Hero CSS geometry。
- 新增 `--color-text-muted`、`--space-5`，並以 1600px、1024px、390px 確認 no horizontal overflow。

### v1.3 — 2026-09-17（working tree）

- 以 approved V2 transparent WebP artwork 取代資產調整 Hero SVG；新增 desktop／medium／small responsive variants 至 `web/src/assets/hero/`。
- 將 desktop artwork render box 擴至 `390px × 220px`，保留既有 container-query 降階與 mobile 隱藏策略。

### v1.4 — 2026-09-17（working tree）

- 使用者頁改為 `AdminPageHero` consumer，與資產調整共用 Hero geometry、icon tile、typography 與 container-query responsive strategy。
- 保留使用者既有 orb artwork 與全部資料查詢、篩選、列表及權限行為。

### v1.5 — 2026-09-17（working tree）

- 登錄產品帳戶範圍收斂為現貨與合約；獨立現貨槓桿 UI／route 不再是可用頁面。
- 明確區分「已移除的現貨借貸」與「合約交易必要的保證金／槓桿」兩個不同領域，避免後續 UI 任務誤刪合約風控資訊。

### v1.6 — 2026-09-17（working tree）

- 修正資產劃轉頁的導航入口，`AssetSectionNav` 現在明確列出總覽／現貨／合約／劃轉；劃轉仍使用既有 `POST /api/v1/assets/transfers` 與 SPOT／FUTURES contract。
- 資產總覽收斂為標題、分頁、帳戶切換、資產明細與帳本歷史，移除重複的 metrics／帳戶 inventory 卡片；資料 API 與資產帳戶切換邏輯保持不變。
- 資產總覽移除第二層帳戶 tab，改由單一資產分頁導航搭配現貨／合約明細卡片呈現，避免總覽內重複導航。

### v1.7 — 2026-09-17（working tree）
- 資產前台顯示文字改由雙語字典提供；中文介面不再直接呈現 `SPOT`／`FUTURES`、帳本參照類型或開發用英文說明，英文介面維持完整英文顯示。
- 劃轉表單保留 `SPOT`／`FUTURES` API 值，只將選單顯示名稱本地化；資產歷史的帳戶類型與參照類型同樣只在顯示層轉換，不改動後端資料契約。
- 客戶端與管理端資產相關文案改用產品語言，移除「唯讀、投影、快照、不可變帳本、adapter、Journal」等不必要的內部術語；資料狀態改以「目前餘額、資料狀態、資產異動紀錄」呈現。
- 客戶端帳戶劃轉表單新增 `admin-assets-transfer` 控制項樣式，沿用管理端資產調整的輸入框／選單高度、圓角、深色表面與焦點狀態；表單欄位與劃轉 API 不變。
- 劃轉表單改為兩欄欄位加一列操作按鈕的三列結構，桌面內容寬度控制在 44rem 內，小於 768px 時自動改為單欄並讓按鈕滿寬；必填欄位沿用管理端紅色星號標示。
- 劃轉表單進一步直接沿用管理端 `.admin-form-select` 的下拉視覺與既有控制項 token，並收斂為 42rem 內容寬度，避免滿版控制項造成視覺鬆散。
- `web/src/pages/assets/AssetTransferRuntimePage.tsx` 現在直接重用 `AdminPageHero`、`AssetAdjustmentHeroIcon` 與既有 V2 asset artwork；客戶端以 `.asset-transfer-page` 局部提供相同 semantic token，並將工作區、導航與操作卡收斂到資產調整相同的深藍 surface、border、spacing 與 responsive 節奏。
- 資產總覽已移除未使用的帳戶容器請求；畫面只以實際呈現的 `/api/v1/assets/balances` 回應決定載入結果，避免非必要的帳戶資料請求失敗而遮蔽餘額總覽。
- 管理端資產工作區的 audit 區塊正式定位為「對賬審計與損益」：用於核對資產流水完整性與異常，並納入營收／虧損帳務核對；尚未實作的完整審計與損益查詢不得以示意數據呈現。

### v1.8 — 2026-09-18（working tree）

- 新增 `/admin/assets/audit` 的 `AssetAdjustmentAuditPanel`，只顯示既有沖銷／空投調整之管理操作、immutable journal 與雙分錄交叉核對結果。
- 新增受 server-side super-admin gate 保護的 bounded read API；缺少雙分錄、借貸不平、資產不一致或目標帳戶分錄時標示 `EXCEPTION`，不執行任何修正。
- 明確登錄手續費收入、營收、虧損與 P&L 尚無可用帳務來源，因此不顯示 mock、零值或預估資料。
- 客戶端資產頁移除「資料狀態」、最後更新與對賬等內部資料品質呈現；使用者只看與操作直接相關的資產、可用、凍結與總額。資料新鮮度仍留在 server／營運監控邊界。
- 資產顯示語彙以「凍結」對應不可用中的資產數量；英文介面對應 `Frozen`。資料欄位與既有 `locked` API contract 保持不變。
- 顯示給使用者與管理人員的時間一律使用 24 小時制（`hourCycle: 'h23'`）；不得以「上午／下午／晚上」或 AM／PM 表示時間。
- 管理端獨立 Vite surface 的 browser title 固定為「LumiX管理後台」；前台仍為「LumiX」，以避免瀏覽器分頁混淆兩個獨立服務。

### v1.9 — 2026-09-18（working tree）

- 管理員密碼重設頁 `web/src/admin/pages/AdminResetPasswordPage.tsx` 改為直接沿用既有 `.admin-login--portal` shell、卡片、語言切換器、密碼控制項、狀態區與 footer；未新增第二套 auth surface CSS，密碼規則、token 提交與導回登入流程維持不變。
- 抽出 `web/src/components/base/CopyButton.tsx`，讓管理端使用者識別值與客戶端個人中心 UID 共用同一個可及複製互動、clipboard fallback 與已複製回饋；個人資料 API 與 UUID 資料契約不變。

### v1.10 — 2026-09-18（working tree）

- `web/src/pages/assets/AssetFundingPage.tsx` 成為 `/assets/deposit` 與 `/assets/withdraw` 的共同方式入口；客戶端資產導覽新增充值與提幣。
- 兩頁提供平台內部、鏈上、OTC 與銀行方式的資訊選擇，但尚未接入 provider、地址派發、銀行結算、OTC 訂單、平台用戶間轉帳或資產異動 command；頁面不得顯示假地址、假餘額、假紀錄或假成功結果。
- 方式入口固定以平台內部轉入／轉出優先，其後才是鏈上、OTC 與銀行；未規劃的「其他」通道不顯示為不可操作按鈕。

### v1.11 — 2026-09-18（working tree）

- `/assets/deposit` 的平台內部轉入現在以 `qrcode.react` 的 SVG QR Code 顯示登入者真實 UUID，並沿用共用 `CopyButton`；QR 只編碼 UUID，不承載鏈上地址、私鑰或其他資產資料。
- `/assets/withdraw` 的平台內部轉出新增收款人 UUID、數量與現貨幣種表單；幣種只來自 `/api/v1/assets/balances` 的 ACTIVE SPOT projection，空資料時顯示受控不可送出狀態，不寫死 USDT 或假餘額。
- 前端命令接至 `POST /api/v1/assets/internal-transfers`。server 以 `PLATFORM_INTERNAL_TRANSFER` 獨立 scope，在單一 transaction 中執行 source HOLD/capture、source DEBIT、recipient CREDIT、投影更新與 immutable audit；此路徑為 `HUMAN_REVIEW_REQUIRED`。

### v1.12 — 2026-09-18（working tree）

- 抽出 `web/src/features/assets/AssetSymbolSelect.tsx`，讓帳戶劃轉與平台內部轉出使用同一個 `.admin-form-select*` 資產下拉；component 不帶固定幣種，所有選項均由呼叫端的真實資料來源提供。
- `/assets/transfer` 從四個兩欄欄位改為來源帳戶、目標帳戶、數量＋資產的三列；同時在帳戶欄位下以真實 projection 顯示目前可用／目前持有，未知或空資料顯示 `—`，不建立假零值。客戶端所有數量控制項的資產下拉只顯示 `assetDisplayName`，不重複附加 API symbol；既有 API endpoint、payload、validation、idempotency 與帳本流程不變。

### v1.13 — 2026-09-18（working tree）

- 平台內部轉出改為收款人 UUID、數量＋資產兩列，不再在桌面版併列欄位；收款人 label 右側新增具 aria-label 的掃碼按鈕。
- 掃碼視窗只使用瀏覽器的 `getUserMedia` 與原生 `BarcodeDetector`，辨識到 UUID 格式 QR Code 才自動帶入並立即停止 camera stream。裝置不支援、權限拒絕或非 UUID QR Code 均維持受控訊息，沒有 fake scan 或推測值。

### v1.14 — 2026-09-18（working tree）

- 使用者管理清單新增凍結登入／凍結提幣與解除操作；所有操作都經確認視窗、後端回讀與既有管理員 cookie session，不在 browser 保存限制狀態。
- 此 UI 對應的 runtime 會留下 immutable audit；登入凍結立即撤銷有效 session，提幣凍結會讓平台內部對他人轉出 fail closed。資產帳本、表單與其他使用者管理查詢不因這個 UI 新增而改變。
- 詳情展開／收合改為保有 `aria-label` 與 `aria-expanded` 的圖示按鈕，避免在高密度使用者列表重複占用文字寬度；凍結登入、凍結提幣與詳情控制項統一為 40px 高度與相同 surface treatment，窄螢幕才允許限制按鈕換行。
- 使用者列表收斂為狀態、限制摘要與單一「管理」入口；詳情改由 `UserManagementDrawer` 的概覽／資產／安全與限制／紀錄 tabs 提供。只有登入與提幣是已接後端的限制能力；平台內轉帳、現貨及合約交易以 disabled capability rows 明示尚未啟用。
