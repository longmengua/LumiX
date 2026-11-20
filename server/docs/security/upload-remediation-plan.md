# 上傳安全最小修補方案

日期：2026-11-20。依據：[上傳安全盤點](upload-security-audit.md)。狀態：計畫完成、runtime 未修改。此處的 MVP 僅指最小安全修補集合，不代表將 LumiX 視為產品 MVP。

## 決策與施工邊界

目前沒有上傳 API，不新增無呼叫者的 validator、假 Storage、掃描 adapter 或新公開入口。先收斂現有框架與匿名圖片生成的攻擊面；未來上傳功能需另外核准 API、ownership、metadata 與部署方案。

本輪只新增報告及 docs 路由。停用 multipart、拒絕 Content-Type、429 admission、依賴更新均會影響公開請求行為或運行相容性，不符合本輪「僅自動實作低風險且不影響公開 API」條件，因此只列方案。無 DB migration、正式設定修改、憑證變更或部署。安全控制變更後續標記 `HUMAN_REVIEW_REQUIRED`。

## 現有系統的最小修補集合（P0）

| 變更 | 風險與證據／受影響位置 | 修改前 → 修改後 | API／DB／部署／相容性 | 驗證與回滾 |
| --- | --- | --- | --- | --- |
| M1：在無上傳需求期間停用 multipart 自動解析，並在 DispatcherServlet 前拒絕非支援 multipart 請求 | U-01 High／U-03 Low；`application-infrastructure.yml:1–6`、ApiAuthenticationFilter.java:69–81；需新增早期 Filter | 框架預設 parser 可啟動 → 明確拒絕且不讀 body／parts | multipart 回應改為明定的 415；JSON 正常行為應保持。無 DB；需核對所有 profile，不能只改 infrastructure 就宣稱全系統完成。不能新增 API 白名單 | 真實 embedded Tomcat 測試確認拒絕發生於解析前；回滾停用設定及 Filter，但回滾會恢復風險，需補入口阻擋 |
| M2：修正受影響依賴，維持 Java 21／Boot 3 方向 | U-01／U-03；`server/pom.xml:9–12` 與 `signer/pom.xml:4`；Tomcat 10.1.26 | 舊容器 → 經當下官方公告核對的受維護 Boot 3／Tomcat 組合 | 無預期 API／DB 改動；實際為跨模組依賴相容性變更，需完整回歸及新映像。不能只升至歷史 CVE 最低修復版即視為安全；本輪不擅自升版 | 離線 dependency tree／有效 POM／完整既有測試；回滾 POM 及已知映像，保持 multipart 阻擋直到再修復 |
| M3：匿名 CAPTCHA admission control | U-02 High；UserAuthenticationController.java:309–312、VisualCaptchaService.java:60–67、SliderCaptchaService.java:54–62 | 每個請求均產圖寫 Redis → 生成前檢查受信任 IP 頻率、全域生成併發與資源預算 | 新增 429／Retry-After 契約；無 DB。IP 必須來自受信任代理鏈，不能直接信任 client header。多 instance 需共享 rate counter；Redis 失效不可放行 | 頻率、併發、失效拒絕、permit 釋放；現有 3 項產圖測試只驗輸出，不重複。回滾控制需保留邊界限流，避免恢復無界入口 |

M1 設定提案（**未套用**）：

```yaml
spring:
  servlet:
    multipart:
      enabled: false
```

單純停用 resolver 不能取代明確的早期拒絕，也不能修復容器其他漏洞。Filter 應使用解析後 media type 比對，不信任大小寫或裸字串前綴；在讀取 parameters／parts／body 前拒絕 `multipart/*`，不記錄內容。錯誤回應沿用既有安全 JSON 格式，不宣稱所有既有狀態碼保持不變。

## 未來上傳的最小安全設計（需先核准 API／DB）

P0 上線門檻，不可因稱為最小修改而省略：

```text
登入／CSRF／owner 授權／admission
  -> Servlet 受限暫存
  -> 實收 bytes 有界複製至私有隔離區（隨機 key）
  -> 副檔名白名單 + Magic Bytes + ImageReader 真實格式
  -> header 寬高檢查（完整解碼前）
  -> 有硬資源上限的圖片處理／掃描
  -> 驗證成功的重編碼輸出
  -> 儲存完成 + DB READY 提交
  -> 每次下載／刪除再次核對 owner + READY
失敗／逾時／掃描不可用 -> 拒絕 + 清理／補償；不可對外公開
```

- 僅允許最後副檔名 jpg/jpeg/png；拒絕路徑分隔符、控制字元、雙副檔名與 SVG/GIF/WEBP/PDF。原檔名只可作經清理的顯示 metadata，絕不作路徑。UUID key 不是授權手段。
- 單檔 5 MiB＝5,242,880 bytes；建議整請求 6 MiB，容納 multipart header，僅允許一個 file part 與明確有界欄位；各層限制 parts/header，不能僅檢查 Content-Length。空檔拒絕。
- Servlet threshold 為 0，location 使用受控私有有配額磁碟。Service 複製仍計數；不要誤稱 MultipartFile.getInputStream 會繞過 Servlet 的預先暫存。對宣告大小與實收大小均檢查；chunked 也必須受限。
- 建議單邊最多 10,000、總像素最多 20,000,000，以 `long` 乘法判斷。先 Magic，再限定 JPEG／PNG ImageReader，以磁碟型 ImageInputStream 讀 header；寬高不合立即 dispose/close，不先 ImageIO.read。header 檢查仍會呼叫解析器，必須有資源邊界。
- 併發用有限 permits、有限佇列；壓滿立即拒絕。20 MP 的單張 ARGB 僅像素就約 80 MB，尚未計 decoder／重編碼／中間 buffer，不能用壓縮檔 5 MiB 推算 Heap。
- 執行緒 Future timeout／interrupt 無法可靠終止卡死解析器。若 P0 要求「硬時間與記憶體上限」，必須在啟用不可信解碼前具備可殺除／重建的隔離程序；獨立 Worker／Pod 的一般優化可列 P1，但硬限制不能延後後還宣稱 P0 完成。
- 掃描若列為安全門檻，只能成功時前進；未連線、逾時、錯誤／未知回應均拒絕。不存在 scanner 時不可假造 CLEAN。重編碼、去 EXIF／附加資料後仍須驗證最終輸出；若原檔與衍生檔都需要掃描，分別記錄 evidence。
- 私有目錄不得由外部使用者寫入 parent，不得掛至 Nginx webroot；CREATE_NEW／NOFOLLOW_LINKS 與受控 parent 一併設計，單純 normalize/startsWith 不能防所有 symlink／競態。限制程序權限及獨立容量，避免啟動時遞迴掃描或刪除用途不明資料。
- DB 記錄 owner、opaque key、大小、實際格式、digest、狀態與處理 evidence；採 QUARANTINE → SCANNING → READY／REJECTED。檔案先可驗證落盤，再以狀態提交發布；DB 失敗不許下載。重試冪等與孤兒回收不能刪除其他請求已引用的檔案。
- 下載與刪除以登入 principal 查 owner，不接受 body userId 決定權限。下載 Content-Type 取伺服器驗證格式，Content-Disposition 用框架 builder／安全顯示名、`nosniff`、私有 no-store；使用串流。READY 不取代每次授權，隨機 key 也不取代 owner scope。

以下僅是 Service 串流計數的關鍵片段，**不是完整可發布的上傳實作**；呼叫端負責私有安全建立、close、例外清理、狀態提交與錯誤轉換：

```java
// 不信任宣告大小；在寫入前限制實收 bytes，避免整份原始檔進 Heap。
long received = 0;
byte[] buffer = new byte[8192];
int count;
while ((count = input.read(buffer)) != -1) {
    if (count > 5_242_880L - received) {
        throw new IOException("檔案超過允許大小");
    }
    output.write(buffer, 0, count);
    received += count;
}
if (received == 0) throw new IOException("不允許空檔案");
```

## P1／P2 與部署提案

P1：具名 ClamAV clamd INSTREAM（內網、scan byte limit、併發、讀寫／連線 deadline、結果嚴格解析）；獨立 Worker queue、死信／重試冪等、每日 owner 容量配額、DB／Storage 對帳與有 grace period 的孤兒回收。多 Pod 使用私有 S3／MinIO 或明確共享儲存，不能假設本地磁碟共用；引入物件服務、secret 與狀態表均為架構變更，先審核。

P2：拒絕原因與掃描／解碼耗時指標、429／timeout／OOM／磁碟容量告警、配額調校與定期解析器更新。Log 僅記 opaque id、原因碼、大小與耗時，不記圖片 bytes、Base64、session／token、完整原檔名或個資。

Docker／Kubernetes／Ingress 均只列方案，本輪不建立或變更正式資源：

- 保留既有 Docker 非 root；pin 已驗證的 JDK patch 與映像 digest。API／Worker 設定 CPU、memory、ephemeral-storage 上限；JVM Heap 留 native／decoder／metaspace 餘裕。
- K8s 設 runAsNonRoot、readOnlyRootFilesystem、drop ALL capabilities、seccomp RuntimeDefault；僅開必要私有暫存 volume，emptyDir sizeLimit 與 ephemeral-storage limits 一致。建立最小 NetworkPolicy，限制 Worker 對 clamd／Storage／必要服務的連線；不能阻斷主 API 已有合法出站依賴。
- 未來上傳 route 才設明確 body 6 MiB、單檔 5 MiB；Ingress controller annotation 必須依實際產品版本確認，不能假定使用某套 Nginx Ingress。保留三服務、port 與 API 隔離。
- Proxy request buffering 自身也可能消耗入口磁碟，需設定容量／連線／body timeout；後端必須防直連繞過入口，service bytes limit 不能省略。

## 離線測試計畫

本輪只跑既有 7 項，無新增測試。新增前先向使用者說明並確認：目的為 MIME／容量／owner／失敗不發布等安全不變量；目前沒有 upload/scanner/storage 測試，與 CAPTCHA 產圖測試不重複。API、狀態模型或 provider 協定演進可能使 fixture／狀態碼斷言需同步調整；盡量驗不變量，避免綁死私有方法或檔案布局。

| 層級 | 後續必要案例與斷言 |
| --- | --- |
| M1 embedded Tomcat | 匿名白名單 POST／有效 session API 的 multipart 早期拒絕，JSON 回歸；確認不呼叫 getParts、不產生暫存；MockMvc 本身不證明容器解析行為 |
| 格式／串流 unit | 正常 JPG/JPEG/PNG；假 Content-Type／Magic；雙副檔名、路徑穿越、控制字元；空檔、5 MiB 邊界／超限、實收超過宣告；檔名不影響 storage key |
| 解碼隔離 | 損毀／截斷圖片、極小檔案巨大像素、單邊上限／乘法溢位；失敗不生成 READY；超時程序被終止及資源釋放 |
| scanner 協定 | mock clamd INSTREAM：威脅、斷線、逾時、超限、錯誤／未知回應都拒絕；測試 fixture 限 test source set |
| MockMvc 授權 | 未登入上傳；跨帳號下載／刪除／替換；非 READY 不可讀；下載 headers；CSRF 按既有 cookie auth 策略驗收 |
| 併發／冪等 | permits 用盡時拒絕、finally 釋放、重複請求不得重複發布／覆蓋、每日容量扣算原子性；mock 時鐘與受控 executor，避免實際耗盡主機 |
| 儲存／DB 補償 | 掃描成功但 DB 寫失敗、寫檔失敗、清理失敗可追蹤；不公開孤兒檔、重試不誤刪已提交檔；私有暫存目錄隔離測試 |

不對 production 發送上述請求。unit／MockMvc 外部依賴可 mock；實際 embedded container 與磁碟生命週期另需本機整合測試。未跑的案例不得標成通過。

## 效能、回滾及完成驗收

停用 parser 可減少不需要的解析；限流會拒絕部分既有流量。落盤、掃描及重編碼增加 I/O／CPU／延遲；容量估算需包含 proxy 暫存、Servlet 暫存、quarantine 與衍生輸出，不能只算一份 5 MiB。先以隔離測試量測每張尖峰記憶體與 p95，再訂併發，不填入未量測的吞吐承諾。

目前文件變更可直接還原三個文件變更，無資料回滾。M1–M3 回滾見表格；未來 DB 新增必須另列 migration 順序與 rollback／保留 evidence 策略，不在本輪推定 schema 或刪除資料。發現安全處理失效時停止接收新檔並保留隔離／稽核資料，不能以重新公開原檔作回滾。

驗收：

1. 現有最小修補：M1–M3 分別審核、實作、通過相應本機離線測試，更新依賴實際解析與部署差異；公開狀態碼變更經確認。需要人工審核。
2. 未來上傳：所有 P0 格式、bytes、像素、硬資源限制、掃描 fail-closed、owner／READY 與失敗清理案例通過，且三層限制可從實際設定證明一致。
3. 多 Pod、scanner／Storage、secret、配額、reconciliation 與 resource policy 必須有真實部署證據；缺少則標記 unavailable，不假成功。
4. 目前階段僅完成稽核與方案；沒有完成上傳安全修補，也不代表 production ready。
