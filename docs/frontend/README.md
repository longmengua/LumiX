# 前端

前端負責呈現狀態，不負責決定資金真相。

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
