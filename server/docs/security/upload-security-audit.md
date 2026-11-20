# C 端檔案上傳安全盤點

日期：2026-11-20。範圍：目前 checkout 的 `server/` 主程式、`signer/`、前端呼叫、migration 與 repo 內部署設定。路徑以 repository root 為準。這是後端技術稽核，並非 production readiness 或安全簽核。

任務狀態：盤點、風險分析及最小方案完成；runtime 修補與新增上傳測試未施工。起始 `git status --short` 為空。指定的 `docs/phases/README.md`、P25 README 與整個 `docs/phases/` 在目前 checkout 不存在，不能更新不存在的 phase status，也不能以其他歷史文件推定實際 runtime。

## 主要結論與版本

**沒有找到 C 端上傳／下載／刪檔實作。不能宣稱發現五個可利用的上傳業務漏洞。** 找到受 multipart 漏洞影響的容器依賴，以及與圖片資源消耗相關的匿名 CAPTCHA 入口；實際部署與攻擊可達性仍有待查事項。

| 項目 | 證據／結果 |
| --- | --- |
| Spring Boot | `server/pom.xml:9–12`：3.3.2；signer 同為 3.3.2 |
| Java | `server/pom.xml:22–25`：Java 21；本機 `java -version` 為 Homebrew OpenJDK 21.0.7 |
| 實際 Maven 解析 | 離線 dependency tree：Spring Framework core/web/webmvc 6.1.11、Tomcat embed core/el/websocket 10.1.26 |
| 圖片函式庫 | runtime 只找到 JDK ImageIO／AWT；未直接宣告 Tika、ClamAV、ImageMagick、Thumbnailator、TwelveMonkeys |
| 容器 JDK | `devops/server/Dockerfile:2,13`：Temurin 21 的浮動 tag；部署映像 digest、JDK patch 待查，不能以本機版本替代 |
| 部署 | 有 server/web/admin-web Dockerfile、兩份 Nginx；Compose 目前只有 PostgreSQL、Redis、Kafka |
| 未找到的部署檔 | 已含 hidden files 搜尋：無 Kubernetes Deployment、Ingress、Helm Chart、GitHub Actions workflow 或 Jenkinsfile；不代表外部環境不存在 |

## 搜尋方法與完整可辨識呼叫鏈

以 `rg` 搜尋主程式與測試的 mapping、MultipartFile、Part、RequestPart、InputStream、getInputStream、getBytes、transferTo、Files.copy/write/readAllBytes、ImageIO、ImageReader、BufferedImage、Tika、ClamAV、ProcessBuilder、Runtime.exec、Resource、StreamingResponseBody、Content-Disposition；另查前端 upload/avatar/download/FormData 類路徑、migration 與部署設定。未讀取本機 secret 檔，也未對運行中的服務發送攻擊請求。

```text
預期的 C 端上傳鏈
瀏覽器 -> 上傳 Controller [未找到]
        -> 上傳 Service [未找到]
        -> 私有 Storage [未找到]
        -> 檔案 DB metadata / 狀態 [未找到]
        -> 圖片解碼 / 掃描 [未找到]
        -> 下載 / 刪除 API [未找到]

實際的圖片產生鏈（不是上傳）
GET /api/v1/auth/captcha/challenge
  -> ApiAuthenticationFilter：精確匿名放行
  -> UserAuthenticationController.createCaptchaChallenge
  -> VisualCaptchaService.create
       -> SliderCaptchaService.create：280 x 144 背景及固定大小拼圖
       或 -> ICON_MATCH：64 x 64 圖示
       或 -> IMAGE_GRID：80 x 80 圖塊
  -> Redis：隨機 challenge key + 答案 + 瀏覽器摘要，TTL 2 分鐘
  -> ImageIO.write(PNG) -> ByteArrayOutputStream -> Base64 -> JSON
  -> 瀏覽器顯示；沒有圖片檔案 Storage 或圖片 DB 寫入

框架仍可能存在的非業務 multipart 解析鏈
匿名白名單 POST 或已驗證 API 請求
  -> DispatcherServlet.doDispatch
  -> checkMultipart -> StandardMultipartHttpServletRequest -> request.getParts
  -> handler 選擇 / Content-Type 拒絕
```

最後一條鏈由本機 Spring 6.1.11 JAR 的 `javap -c -p` 檢查確認：`checkMultipart` 在 `getHandler` 前，wrapper 會呼叫 `getParts`。Boot 預設解析條件及部署覆寫仍須 runtime 核對；沒有上傳 Controller 不代表不會啟動 multipart 解析。一般受保護 API 會先經 session filter；匿名 POST 清單見 `ApiAuthenticationFilter.java:40–52,69–81`，不能將所有 URL 都描述為匿名可達。

其他命中已排除：`getBytes` 主要用於文字雜湊；行情 `ofInputStream` 是後端對 Binance 的出站 HTTP；signer 讀取受設定控制的 keystore，不是 C 端上傳。`ImageIO.read` 僅在既有 CAPTCHA 測試讀取後端生成圖片，不能當作不可信圖片解碼漏洞。

## 優先審查的五項發現

分級為本專案處置優先度，並非重算 CVSS。確認依賴命中與確認部署可利用必須分開。

| ID | 分級與性質 | 程式碼證據 | 可利用前提、影響與現有防護 |
| --- | --- | --- | --- |
| U-01 | High：確認受影響依賴；部署利用待驗證 | `server/pom.xml:9–12,36–39` 引入 Boot web；離線 tree 為 Tomcat 10.1.26；`application-infrastructure.yml` 未覆寫 multipart | CVE-2025-48988：大量 parts 引發記憶體耗盡。需要請求到達 multipart parser；匿名 POST 白名單提供可能路徑。總 bytes 限制不等同 parts/header 限制；未執行 PoC |
| U-02 | High：確認應用層缺少 admission control；外部防護待查 | `UserAuthenticationController.java:309–312`、`ApiAuthenticationFilter.java:78–87`；`VisualCaptchaService.java:60–67,110–138,175–178`；`SliderCaptchaService.java:54–62,138–160` | 匿名可反覆產圖及新增 Redis key；未見建立題目的 user/IP rate limit、全域生成併發或容量預算。CPU／Heap／Redis 可受持續請求壓力；固定尺寸與 TTL 是既有防護，但不限制建立速率。無壓測，不能聲稱已實測 OOM；範圍為相鄰圖片入口，非上傳漏洞 |
| U-03 | Low：確認受影響依賴；條件式磁碟 DoS | 同 U-01；未見自訂 multipart location 或 cleanup | CVE-2025-61795：multipart 錯誤後暫存檔延遲清理。需要 parser 寫入暫存並發生錯誤，且累積超過回收速率；實際 temp 目錄、磁碟容量及 GC 條件待查。不是已確認業務孤兒檔案漏洞 |
| U-04 | Medium：潛在部署／未來功能風險 | `devops/web/nginx.conf:20–26` 無局部 body limit；`server/src/main/resources/application-infrastructure.yml:1–30` 無 multipart policy；未找到上傳 Service | 無 repo 級一致上傳大小契約。Nginx 預設 body 1m；Boot 預設單檔 1MB、整請求 10MB，不能說目前無限制。若未來允許 5 MiB，上游會先拒絕；直連後端限制也不同。實際 Nginx include、環境覆寫待查 |
| U-05 | Medium：潛在部署隔離風險 | `devops/server/Dockerfile:13–24` 已有非 root，但 repo 無 K8s／worker resource policy；上述產圖在 API JVM 同步執行 | 匿名產圖及可能 multipart 解析共享 API 資源。部署 CPU／記憶體／暫存磁碟上限、連線與全域併發控制無法確認；不可直接判定 production 缺少這些限制。未來不可信解碼需硬性隔離與程序終止機制 |

沒有 Critical 的已確認發現。U-01、U-03 屬同一容器解析邊界的不同失敗模式，不代表找到兩個業務上傳入口。未發現可證實的上傳 RCE、路徑穿越或檔案 IDOR。

Tomcat 受影響範圍與個別修復版本依 [Apache 官方公告](https://tomcat.apache.org/security-10.html)：CVE-2025-48988 影響至 10.1.41，10.1.42 修復；CVE-2025-61795 影響至 10.1.46，10.1.47 修復。這些是個別 CVE 的最低修復版本，不是建議部署的完整安全基線。大小與落盤預設依 [Boot 3.3 MultipartProperties](https://docs.spring.io/spring-boot/3.3/api/java/org/springframework/boot/autoconfigure/web/servlet/MultipartProperties.html)；Nginx 預設依 [官方 client_max_body_size](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size)。

## A–O 檢查結果

| 項目 | 結果 |
| --- | --- |
| A 副檔名／路徑／symlink | 無使用原始上傳檔名組路徑的入口；無法驗證不存在的 Storage 行為 |
| B MIME／Magic／真實格式 | 尚無上傳驗證器；不是已確認可繞過 |
| C／D 解碼像素及炸彈 | runtime 無 ImageIO.read 或 ImageReader 解碼外部圖片；生成尺寸固定 |
| E Heap 複製 | 無 MultipartFile.getBytes／Files.readAllBytes；CAPTCHA 有有界單次 PNG/Base64 複製，總併發仍見 U-02 |
| F 並行耗盡 | U-01／U-02／U-05；未做壓測 |
| G 多層大小限制 | U-04；現有預設非無上限 |
| H 解析器版本 | Tomcat CVE 見 U-01／U-03；未完成完整 SCA／JDK CVE 盤點，不能宣稱其餘依賴安全 |
| I Shell／執行檔 | 未找到上傳檔案執行、ProcessBuilder 或 Runtime.exec 路徑 |
| J 掃描失效 | 無 ClamAV 整合，也無上傳發布路徑；不能描述為已有掃描 fail-open |
| K 所有權／覆蓋 | 無檔案下載／刪除。既有 profile 以登入 principal 定位，見 UserProfileController.java:34–51 |
| L webroot／執行 | 無上傳目錄；infrastructure profile 關閉靜態 resource mappings（設定第 4–6 行）。Nginx webroot 是前端 bundle，未找到共用上傳掛載 |
| M 暫存／補償 | U-03；無檔案 DB transaction，不能聲稱已有 DB／檔案不一致 |
| N Log | 無上傳內容 Log 路徑；未稽核外部 access log 或 APM payload capture |
| O 下載 Header | 無下載 API。TransportSecurityFilter.java:54–60 已提供 no-store／nosniff 等；無法替不存在的下載驗收 Content-Disposition |

檔案命名、公開 URL、所有權、頻率配額、READY gate、清理均未找到業務實作。不能假定 S3／MinIO、ClamAV 或私人隔離區已存在。

## 驗證及限制

本次離線執行：

```sh
./mvnw --offline dependency:tree '-Dincludes=org.apache.tomcat.embed:*,org.springframework:spring-web*,org.springframework:spring-core'
./mvnw --offline -Dtest=SliderCaptchaServiceTest,VisualCaptchaServiceTest,SecurityBoundaryTest test
```

結果：dependency tree 成功；7 tests，0 failures／errors／skipped。兩組 CAPTCHA 測試 mock Redis；SecurityBoundaryTest 是分類／錯誤契約測試，並非 session filter 整合驗證。未新增測試、安裝依賴、修改 lockfile、呼叫 ClamAV、連 DB 或對正式服務進行攻擊測試。

待查：運行 profile、實際 JAR／映像 digest、環境設定、Nginx 完整配置、外部 WAF／Gateway／Ingress、公開網路拓撲、暫存目錄與權限、容器限制。未做 multipart 可達性、並行 OOM 或磁碟殘留實測。完整上傳測試清單是後續方案，不能視為已通過。
