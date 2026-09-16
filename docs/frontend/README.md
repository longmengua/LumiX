# 前端

前端負責呈現狀態，不負責決定資金真相。

## UI 設計入口

- [LumiX Institutional Blue](../governance/lumix-institutional-blue.md)：管理後台視覺權威、AI 修改邊界與驗收流程。
- [UI 長期治理與實作同步文件](../ai/LUMIX_UI_GOVERNANCE.md)：canonical 元件、token、頁面實作快照、已知視覺缺口與 UI task 同步責任。
- [元件與樣式盤點](ui-components.md)：現存 React／CSS／SVG 來源及重用方式。
- [語意 token 契約](ui-tokens.md)：現有 variables、待導入 token 與漸進遷移規則。

此處既有資料表 Flexbox 規則、`state-management.md` 與 `ux-safety.md` 繼續生效；設計文件以連結引用，不重寫其業務規則。

```text
page-map.md          route and page ownership
state-management.md client state rules
ux-safety.md         trading and wallet UX safety
```

## 管理端資料表布局規範

- 管理端資料表與可展開的資料列必須使用 Flexbox，不得使用 CSS Grid。
- 欄位寬度以 `flex` / `flex-basis` 表達；主要識別欄吸收彈性空間，固定資訊欄與操作欄保留可預期寬度。
- 寬螢幕的列可設定最大寬度並水平置中；窄螢幕保留橫向捲動，不以 Grid 強塞欄位而造成內容擠壓。
- 這項規範只適用資料表與可展開資料列；一般頁面、表單與卡片布局依內容選擇合適技術。
