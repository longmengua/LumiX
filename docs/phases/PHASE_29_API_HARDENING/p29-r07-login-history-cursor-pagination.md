# P29-R07 — 登入紀錄 Cursor 分頁與雙向捲動

## 狀態

```text
COMPLETED_FOR_AUTHENTICATED_PAGINATION_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

讓登入紀錄以每頁預設 10 筆的方式讀取，向下可取較早紀錄、向上可取較新紀錄；使用者查看過去日期後重新整理，仍要以同一筆登入時間為窗口起點，而不是被拉回最新登入紀錄。

## 已納入範圍

- `GET /api/v1/account/login-history?limit=10`：最新 10 筆成功登入紀錄。
- `before=<occurredAt>`：取得該時間之前的較早紀錄；`after=<occurredAt>`：取得較新的紀錄；`anchor=<occurredAt>`：取得包含該時間的刷新窗口。三者互斥，limit 範圍為 1–50。
- response 保持 `records[].occurredAt` 與 `hasOlder`、`hasNewer`；P29-R08 僅向本人追加 session 建立時的 `ipAddress`、去敏 `deviceLabel`，不回傳 session ID、secret digest、Cookie、完整 User-Agent 或密碼。
- SQL 使用 `user_id` 與 `created_at` cursor，不使用 offset，避免新登入事件插入後使已查看頁面位移。
- 前端登入紀錄頁以頂端／底端 sentinel 觸發較新／較早頁面；向上 prepend 時回補文件高度，避免閱讀中的列跳動。
- 前端會將目前視窗最上方可見紀錄寫入 URL `anchor` query；重新整理時先讀取該 anchor，之後仍可雙向載入。

## 資料與安全邊界

```text
目前登入者 HttpOnly Cookie
        |
        v
authentication filter 注入本人 principal
        |
        v
GET /api/v1/account/login-history
        |
        +-- before：較早頁
        +-- after ：較新頁
        +-- anchor：刷新定位頁
        |
        v
user_sessions（同一 user_id 的成功 session created_at）
```

cursor 只表示已向本人公開的 `occurredAt`，不是 userId 或任何 session 認證材料。使用 primary transaction，確保剛建立的 session 可立即出現在新頁；P29-R08 的 IP／裝置快照只供本人安全辨識，仍不是完整 security event 或 session 管理系統。

## 明確不含範圍

- session 撤銷 UI、裝置管理 UI、MFA、異常登入偵測、地理位置、SIEM 與完整 security audit event。
- 任意帳號查詢、userId query、session ID／token／Cookie 暴露。
- KYC、資產、交易、帳本、入金、提款、production launch 或 production-ready 宣稱。

## 驗證

```text
2026-09-10
PASS  web npm run typecheck
PASS  web npm run build
PASS  Docker server image build（含 Java 編譯）
PASS  Docker Compose 重建 server/web 並健康啟動
PASS  建立 12 次以上成功登入：初始 limit=10 回傳 10 筆、hasOlder=true、hasNewer=false
PASS  anchor 讀取：包含 anchor 本身，並回傳 hasNewer=true，可支援刷新定位
PASS  after 讀取：回傳 anchor 上方較新資料且按 DESC 顯示
PASS  before 讀取：回傳 anchor 下方較早資料且按 DESC 顯示
PASS  未帶 Cookie：登入紀錄 cursor API -> 401
PASS  git diff --check
BLOCKED  本機 ./mvnw test：使用者 Maven settings 指向的 Nexus 連線逾時；此為環境相依下載失敗，不是 assertion 失敗
```

## Rollback

不需要 migration rollback。相容 application 可忽略新的 query 與 response flags，回到既有最新紀錄讀取；不得為了 rollback 刪除 `user_sessions` 或登入歷程。

## 人工審核重點

`HUMAN_REVIEW_REQUIRED: yes`，因為擴充 authenticated private API。請檢查：cursor 只能作用於 request principal、三種 cursor 互斥、limit 有上限、response 不含 session 認證材料、URL anchor 只包含成功登入時間，以及 prepend 的捲動補償不會改變使用者目前閱讀的位置。
