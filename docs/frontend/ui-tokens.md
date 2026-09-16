# LumiX UI 語意 token 契約

本文件是 [設計治理](../governance/lumix-institutional-blue.md) 的工程映射。**以下「待導入」名稱是規格，不是已存在於產品的 variables。** 本輪只改文件，未建立 runtime tokens 或遷移 CSS。

## 已有 token 與缺口

來源為 `web/src/styles/global.css`：

| 已有來源 | 現況／用途 |
| --- | --- |
| `:root --content-max` | 90rem，頁面容器上限 |
| `--content-gutter` | clamp(1rem, 2vw, 2rem)，頁面側邊距 |
| `--section-gap` | clamp(1rem, 2vw, 1.5rem)，區段間距 |
| `--panel-min` | min(100%, 18rem)，面板限制 |
| `--logo-mark-size`／`--logo-font-size` | logo variant 範圍，不作一般 Hero token |
| `--asset-adjustment-icon-*` | 資產 Hero icon 局部色彩，尚非跨頁色票 |
| `--users-page-padding` | 使用者頁局部 spacing，不應複製成每頁一套 |
| 字體 | `:root` 的 Inter／system stack；無完整語意字級 token |

目前未發現獨立 Tailwind config 或 theme package；不應在實作中引用不存在的 Tailwind class、色票 API 或宣稱 token 遷移完成。

## 待導入的共享語意

以下為後續 UI 實作可採用的中央契約；初始值應從既有穩定後台 surface 收斂並以截圖驗證，而非套用另一個品牌預設色。視覺政策及禁止風格只在治理文件維護。

| 待導入名稱 | 角色／選值方向 |
| --- | --- |
| `--color-bg-primary` | 頁面深 navy／midnight 主底 |
| `--color-bg-surface` | 較主底略亮的 panel |
| `--color-bg-control` | 表單輸入底 |
| `--color-border-subtle` | 低對比 cool-blue 邊框 |
| `--color-border-focus` | 可辨識品牌 focus 邊框 |
| `--color-text-primary` | 近白標題與主要資料 |
| `--color-text-secondary` | muted blue-gray 次要說明 |
| `--color-text-muted` | 輔助及 metadata，仍須可讀 |
| `--color-accent-primary` | institutional blue 主操作色 |
| `--color-accent-secondary` | indigo／violet 輔助品牌色 |
| `--color-state-success`／`--color-state-warning`／`--color-state-danger` | 延續 Badge 與錯誤語意，不以品牌藍取代 |
| `--gradient-hero`／`--gradient-accent` | 控制方向與 stop 的共享漸層 |
| `--shadow-surface`／`--glow-accent` | surface 陰影與低強度局部光暈 |
| `--radius-card`／`--radius-control`／`--radius-pill` | card 約 20–24px、control 約 10–12px、pill 完全圓角 |
| `--font-size-page-title`／`--font-size-hero-title` | 頁面與 Hero 階層；Hero 桌面可約 36–40px，窄容器需縮減 |
| `--font-size-section-title`／`--font-size-body` | 區段約 17–19px，正文約 14–16px |
| `--font-size-helper`／`--font-size-metadata` | 輔助資訊約 13–14px，依可讀性及語系驗證 |
| `--space-1` 至 `--space-8` | 建議 4／8／12／16／20／24／28／32px 階梯，以 rem 表達 |

上述尺寸是收斂起點，不是要求整站立即套版；已凍結的 reference geometry 優先保留。控制項行高、padding 與字級應共同決定高度，不讓字體放大後文字被裁切。

## 漸進導入規則

1. 施工前搜尋已存在 variables；已具相同語意就延伸，不能新增同義 token。既有 `--content-gutter`、`--section-gap` 保留，避免建立第二套頁面間距來源。
2. 有 UI 程式修改授權時，先在現有 `global.css` 的共享 theme 區導入最小必要語意；後台限定色彩可作用於 `.admin-layout`，跨前後台需另查影響。若實際 class 已改，先確認 `AdminLayout` 的根元素。
3. 漸層 stop 與 SVG fill／stroke 引用同一語意色系；共用 palette 不能每頁再宣告私人 hex。插圖特殊高光可用受控透明度／局部 alias，說明用途，不擴散成另一套品牌色。
4. 先遷移本輪元件，移除被取代的重複值及覆蓋規則，驗證所有實際 consumers；不做無關全檔搜尋取代。
5. 更新此文件中的實作狀態、CSS 定義位置及 consumers。若有差異記錄理由；只有規格、尚無程式實作時繼續標為待導入。

檢查 normal、hover、focus-visible、selected、disabled、error 與 loading；不要以 alpha 極低的字色追求質感。新增共用 token 需確認深色漸層上的實際對比及既有前後台樣式不回歸。
