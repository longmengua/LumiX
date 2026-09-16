# LumiX Institutional Blue — UI Design Governance

狀態：正式設計治理。適用管理後台；建立於 2026-09-17。本文是視覺設計的唯一權威來源，不代表目前所有頁面皆已符合規範。

## 範圍與權威

本規範將 LumiX 管理後台定位為 Premium Enterprise SaaS、Institutional Fintech 與 Digital Asset Infrastructure：安全、精準、透明、受治理、可追溯、高效率且專業，帶有克制的未來感。

實作入口為 [元件盤點](../frontend/ui-components.md) 與 [token 契約](../frontend/ui-tokens.md)。[前端布局規範](../frontend/README.md)、[狀態管理](../frontend/state-management.md)、[UX safety](../frontend/ux-safety.md) 保持原有責任；[AGENTS.md](../../AGENTS.md) 與 [AI_AGENT.md](../../AI_AGENT.md) 的資料真實性、服務隔離及資金安全規則不由本文重新定義。

衝突處理：先遵守本次明確範圍及既有業務／安全限制，再依本規範收斂範圍內的實作。未明確要求例外時，修正偏離規範的實作，不透過新增頁面私有風格來合理化差異。範圍外差異記錄為後續項目，不以治理為由擴大改動。Golden Reference 是範圍內視覺依據，不是業務功能、API 或權限的來源。

## 決策順序與視覺基調

UI 決策依序考慮：可用性 → 資訊層級 → 一致性 → 可及性 → 視覺品質 → 裝飾效果。此順序不允許犧牲鍵盤操作、對比或錯誤辨識。

主背景採深 Navy／Midnight Blue。表面以 restrained blue／indigo gradient、低強度半透明、薄冷藍邊框、柔和內部光暈及受控邊緣光建立層次。先靠表面亮度、留白與字級區分內容，再使用邊框與光影，不讓每層 container 都有亮框。

禁止 Cyberpunk、Gaming UI、過量 neon、彩虹漸層、裝飾堆砌、過度模糊／玻璃感，以及寫實人物、Anime、Mascot、Meme 風格。成功／警告／錯誤仍維持語意色，不為統一藍色而抹去狀態差異。

## Page Hero pattern

有 Hero／Page Header Card 的主要功能頁，於完整 Hero 工作範圍內優先採用以下結構：

```text
左：功能 icon + 標題 + 描述 + 2–3 個功能 chips
中／右：與功能相關的抽象立體插圖
最右：價值主張 + 輔助說明
```

Hero 同時提供頁面辨識、功能脈絡與產品品牌。不是每頁都必須新增 Hero，也不應重複既有外層頁名。`admin-users-hero` 與資產表單 header 是現有實作參考，不代表已存在可直接 import 的共用 `PageHero`。

- Icon 採藍／indigo 漸層圓角方塊、柔和內光與邊緣光，中央使用與功能直接相關的簡潔線條。Hero 主視覺需有足夠重量，但不可呈现按鈕 hover／pointer 語意。
- Chips 為半透明、薄框、pill shape、可配小線條 icon 的次要資訊，不是 CTA。使用者頁可用「帳號管理／登入紀錄／權限控管」，資產調整可用「沖銷作業／補正處理／審核留痕」；這些僅為文案候選，必須確認現有功能支持，不能宣稱不存在的能力。
- 局部 icon／插圖 refinement 不順便加入 chips、改標題或 description。既有明確要求無 description 的頁面保留該例外，直到使用者授權更動。
- Slogan 與輔助文字使用既有 i18n，不用「合規」等品牌文案暗示通過認證、上線 gate 或不存在的審核程序。

## Illustration 與裝飾

採 Abstract Institutional 3D Illustration：以 glossy blue／indigo、半透明材質、柔和反射與受控 glow 呈現功能抽象圖。可用 users、shields、coins、wallets、documents、cubes、data blocks、adjustment arrows、network nodes、orbit lines、floating spheres；每個元素都必須支持頁面語意。

本 repo 現有 inline React SVG 適合簡潔 pseudo-3D：以 filled shapes、漸層、coin 頂面、傾斜 card、少量陰影建立深度。不要以 outline-only icon 取代已指定的立體構圖，不使用 emoji／文字箭頭冒充圖示。優先修改既有 SVG，不為視覺工作新增大型依賴、外部圖 URL 或 base64。SVG defs 在可重複掛載元件中須使用唯一 ID，裝飾圖設 `aria-hidden` 且不可搶焦點。

局部 spotlight 應置於物件後方，限制在插圖區，不能藉此重畫整個 Hero 背景。使用最少必要 filter；高光集中於主物件，背景球體降低亮度／透明度，ground 僅承托，不搶過 card、coins 或頁面資訊。

曲線光軌、orbit、點陣、radial glow、粒子、grid 只允許低強度；裝飾不能遮住文字、欄位、選單、focus ring 或捕捉操作事件。不預設新增動畫。

## Typography、surface 與間距

保留 `global.css` 的 Inter／system font stack；字級階層依序為 Page Title、Hero Title、Section Title、Body、Helper Text、Metadata，映射見 token 契約。標題可近白，輔助資訊使用 muted blue-gray；不讓所有文字都純白。

Surface 使用深色半透明底、克制漸層、薄藍框、柔和內光及低強度陰影；卡片圆角一致，避免厚重陰影、強 neon outline、過度 blur 或過亮底色。相同層級使用同一 spacing／radius token；例外須源於內容或已批准的 reference，而非逐頁任意數值。

## Forms、dropdown、buttons、tables

- Input／Select／Textarea 採 dark surface、薄 blue-gray 邊框、明確 focus、適中圓角和最小 glow。同列 control 高度一致，label 不以 placeholder 取代，required／錯誤需有文字與欄位關聯。
- Dropdown 沿用 `.admin-form-select*` 的深色視覺：選取高亮、check、低對比 hover、明確展開狀態。這是統一外觀方向，不表示既有局部 `FormSelect` 的可及性已全面驗證；共享前須檢查 Arrow Up／Down、Enter／Space、Escape、Tab、focus 返回、ARIA 與 disabled。不要只複製選單 CSS。
- Primary button 採 institutional blue 或克制藍漸層；secondary 採深色半透明與薄框。保留 loading、disabled、防重送及既有 confirmation，不藉視覺修改添加資產操作流程。
- Tables 先確保辨識、對齊、列分隔、數值精度與操作可達，避免巢狀 card。管理資料表／展開列仍依前端 README 使用 Flexbox；必要表格橫向捲動不等於允許整頁溢出。

## AI 修改流程

1. 讀本規範及任務必要的既有規則，執行 `git status --short`，記錄既有變更；確認 route、頁面、reference 路徑及允許修改／凍結區域。
2. 檢查 shared components、theme／CSS variables、typography／spacing／radius、Hero、icon、responsive；核對 API 資料來源、validation、permission、loading／reset／錯誤／成功狀態。只讀這些流程，不因美化改行為。
3. 依 reuse > extend > create 選方案。先調整視覺層級、Hero、spacing、typography、surface、border，再處理 illustration 與微裝飾。局部任務仍以其指定區域為先。
4. 多頁同 pattern 優先擴充共用元件；若尚無元件且本輪只有單頁局部修正，先維持局部改動並記錄共用缺口，不做大規模抽取。禁止複製一整套頁面 CSS 建立第二個版本。
5. 有 reference 時先截圖基線，再實作 → 同尺寸截圖 → 比對 → 修正 → 最終截圖。比較 geometry、spacing、對齊、surface、border、typography、icon／illustration；不得把參考圖的其他導航當修改授權。
6. 驗證實際可用容器，不能只用 viewport 推測：桌面 1600／1440、窄桌面 1280、平板 1024／768、mobile 約 390，依影響範圍選必要尺寸。固定 sidebar、長文案、語系及縮放皆會減少可用寬度；插圖先縮小或依既有策略隱藏，不遮蔽標題／操作。不以 `overflow-x: hidden` 或 scrollWidth 相等冒充沒有重疊；需檢查截圖及元素邊界。凍結 Hero 高度時須比對實際高度，CSS 未改不代表渲染高度未變。
7. 功能檢查與此次變動風險相稱；UI fixture 只可用於隔離測試並明確揭露，不能拿它證明真實 API、permission、audit 或資產 mutation 正常，不得為截图修改 production authentication。
8. 查看當前 `web/package.json` 再跑現有檢查。盤點時有 `typecheck`、`build`，無 lint／test script；缺少就如實報告，不宣稱通過。後台 build 使用 `VITE_LUMIX_SURFACE=admin npm run build`，開發使用 `npm run dev:admin`。純文件任務檢查 diff、路徑及規則一致性即可。
9. 回報改動範圍、重用／延伸決策、驗證、尚存差異與例外；有不合格的重疊不得宣稱全數驗收完成。提交僅含本任務授權內容，遵守既有 staging 規則。

## 維護規則

本規範的視覺政策只在此維護；入口文件放摘要與連結，元件文件記錄「在哪裡」，token 文件記錄「如何映射」。修改共用 pattern 或 token 時同步更新對應文件、檢查受影響頁面，避免改一頁卻影響前台。

新 reference 若成為長期權威，記錄其用途、版本及凍結範圍；不把易更名的根目錄截圖當唯一規範。不將現有視覺缺陷或未實作 token 記為完成。本輪只建立治理文件，不改產品碼、不重新宣稱 P30 runtime 或 production readiness。
