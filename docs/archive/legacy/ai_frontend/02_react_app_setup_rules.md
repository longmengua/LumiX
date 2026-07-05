# 02 React App Setup Rules：React 專案規則與目錄約定

## 任務

建立 React + TypeScript 前端工程規則文件與目錄約定。  
如果專案已有 docs 目錄，請放入 docs；否則建立 docs。

---

## 技術固定

```text
使用 React。
使用 TypeScript。
優先 Vite。
不要改成 Next.js。
不要使用 Vue / Nuxt。
不要新增大型狀態管理，除非 repo 已經使用。
不要新增大型 UI 套件，除非使用者確認。
```

---

## 建議目錄

```text
src/
  app/
  routes/
  pages/
  components/
  features/
  services/
  mocks/
  hooks/
  utils/
  styles/
  types/
```

---

## 功能分層

| 層 | 說明 |
|---|---|
| pages | 路由頁面 |
| components | 共用 UI |
| features | 交易所業務元件 |
| services | API client / mock service |
| hooks | React hooks |
| utils | 格式化、脫敏、計算 |
| types | TypeScript 型別 |
| mocks | mock data |

---

## 必要規則

```text
頁面不得直接寫死大量 mock，mock 放 services 或 mocks。
資產、價格、數量統一用格式化工具。
API key、Email、手機、地址必須脫敏。
交易頁開發期可先用 mock，不接真實 WebSocket；OL 前必須接真實 API / WebSocket。
高危操作必須有 ConfirmDialog 或 SecurityVerifyModal。
每個頁面要有 loading、empty、error 狀態。
```

---

## 需建立文件

| 文件 | 內容 |
|---|---|
| docs/FRONTEND_RULES.md | React 前端規則 |
| docs/ROUTES.md | 路由規劃 |
| docs/COMPONENTS.md | 共用元件規劃 |
| docs/MOCK_API.md | mock service 規則 |

---

## 驗收標準

```text
建立 React 前端規則文件。
建立路由與元件規劃。
明確禁止改成 Next.js / Vue。
明確要求開發期 mock service，並在 OL 前切換成真實 API / WebSocket。
明確要求 loading / empty / error。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```
