# P29-R09：滑動驗證碼與註冊 Bloom filter 預檢

## 任務目的

降低登入、註冊與忘記密碼入口被自動化濫用的成本，同時讓註冊 email 的重複預檢不必每次都查詢 primary database。這是 authentication／abuse-admission 強化，不是帳號風控、rate limit 或 production launch 宣告。

## 完成內容

- `GET /api/v1/auth/captcha/slider` 建立 280×144 server-side 拼圖；答案只存 Redis，response 只有圖片、challenge id 與拼圖尺寸，沒有 target X。
- `POST /api/v1/auth/captcha/slider/verify` 原子消耗 challenge，驗證位移與 browser fingerprint 後，核發 5 分鐘、用途限定的高熵通行 token。
- `POST /api/v1/auth/register`、`/login`、`/password/forgot` 都必須消耗對應用途 token；challenge、token、用途不符、fingerprint 不符或重放一律拒絕。
- Redis 連線故障時驗證碼流程回 `SERVICE_UNAVAILABLE`，不會降級為未驗證也能執行帳號動作。
- React 共用 `SliderCaptcha` 元件在三張 auth 表單按下送出後才開啟彈窗，再向 server 取得題目；使用者放開滑塊即送位移至 server 判定，成功後自動接續原本表單提交，沒有第二個「完成驗證」按鈕。token 僅在接續提交的記憶體呼叫中使用，不能寫入 URL、localStorage 或 sessionStorage。
- `RegistrationEmailBloomFilter` 以 Redis bitmap 的六組 SHA-256 衍生 offset 進行「可能存在」預檢；命中後仍必須查 primary database。資料庫 `users.email` unique constraint 永遠是唯一的最終裁決。

## 安全不變式

```text
瀏覽器  --取得圖片-->  Redis challenge（target X + fingerprint，TTL 2 分鐘）
瀏覽器  --回傳位移-->  原子 GETDEL challenge
                              |
                              +--失敗／重放--> 拒絕
                              |
                              +--成功--> Redis pass token（purpose + fingerprint，TTL 5 分鐘）
                                                |
帳號 endpoint --原子 GETDEL pass token----------+
                              |
                              +--用途／fingerprint 不符或重放--> 拒絕
                              +--相符--> 執行原本帳號流程
```

fingerprint 是 User-Agent、Client Hints 與 Accept-Language 的摘要，並不是硬體身分，也可能被高能力攻擊者模擬。因此它只作為偷取通行 token 的附加限制；真正的核心仍是高熵 token、短時效與 Redis 原子消耗。

Bloom filter 允許 false positive，絕不允許據此拒絕註冊。Redis 重啟或失效會造成 false negative，流程自然回到 database unique constraint，不能犧牲正確性換取快取命中率。

## 第三方與相容性決策

檢視 captcha-pro 的 Spring Boot 3 MIT reference implementation 後，未直接引入其 starter 或前端 package；LumiX 只採其「圖片題目由 server 生成、答案由 server 保存」的安全邊界，使用 JDK image API 與既有 Redis topology 實作，避免把不必要的 framework dependency 接入 auth path。Redis access 一律透過既有 `RedisTemplate`，因此 standalone 與 cluster deployment 共用相同程式。

## 驗證

```text
PASS  server ./mvnw -q -DskipTests compile
PASS  web npm run typecheck
PASS  web npm run build
PASS  Docker Compose rebuild，server / web health check
PASS  取得 slider challenge：回傳 PNG data URI、尺寸與 id，沒有 target X
PASS  錯誤位移 -> 400 CAPTCHA_INVALID
PASS  LOGIN token 用於 REGISTER -> 400 CAPTCHA_INVALID
PASS  PASSWORD_RESET token 首次消耗 -> 202；同一 token 重放 -> 400
PASS  原本使用記憶體資料庫的 migration／ledger runtime tests 改為直接連 PostgreSQL 16.4 的隔離 `lumix_test_*` schema，V001–V011 可完整套用
PASS  Spring context smoke test 使用 PostgreSQL integration profile，不再依賴 embedded database
KNOWN  server 全量測試：400 項中 7 項既有 architecture guardrail 因舊 phase token allowlist 未納入已存在的 auth runtime 而失敗；沒有 H2、migration 或 PostgreSQL assertion error
```

## 明確未完成

- IP／帳號 rate limit、異常足跡、bot detection telemetry、IP reputation、WAF、SIEM 與人工風控。
- 視障使用者的非視覺等價驗證方案；上線前必須另行設計且避免降低防護強度。
- CSRF、TLS ingress、HSTS、secret manager、production security review 與 production launch。

## Rollback

application rollback 可停止要求 captcha token 並移除前端元件；Redis 中的 `lumix:auth:slider-captcha:*` 暫存資料會依 TTL 自然失效，沒有 migration 或使用者永久資料需要刪除。不得移除 `users.email` unique constraint，也不得把 Bloom filter 當作資料正確性來源。

## 風險與人工審核

`HUMAN_REVIEW_REQUIRED: yes`，因為改變匿名 authentication endpoint admission。人工審核應確認：所有三個入口都在業務動作前消耗用途 token、Redis 故障 fail-closed、answer 不會進 response 或 log、challenge/token 不可重放、跨用途與跨 fingerprint 皆被拒絕，以及 Bloom filter false positive 不會造成錯拒。
