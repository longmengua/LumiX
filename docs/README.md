# LumiX 文件導覽

這一頁像整本書的目錄。你可以先在這裡看懂每一章在講什麼，再往下讀細節。

## 章節一覽

- `architecture/`：系統怎麼分層、資料怎麼流、交易與錢包的邊界在哪裡。
- `product/`：LumiX 要提供哪些產品能力，使用者與營運人員會怎麼操作。
- `frontend/`：前端畫面怎麼分、每一頁的用途是什麼。
- `backend/`：Java 後端怎麼分工、哪些是服務邊界、哪些是資料邊界。
- `exchange-core/`：交易所真正的核心能力，像是帳本、凍結、撮合、結算、錢包與風控。
- `operations/`：部署、監控、事故、上線與維運要怎麼做。
- `phases/`：Phase 12 到 Phase 36 的章節規格，每章各自說明要做什麼。
- `ai/`：Codex 的工作規則、流程與提示詞索引。
- `archive/`：歷史資料、舊版規劃與已歸檔內容。

## 先讀哪幾章

1. `ARCHITECTURE_TEXT_MAP.md`：先看整體長什麼樣。
2. `architecture/data-and-event-flow.md`：看系統層級的資料與事件流。
3. `architecture/order-settlement-flow.md`：看訂單、預留、撮合、結算與對帳。
4. `OPERATING_EXCHANGE_MASTER_PLAN.md`：再看目前做到哪裡、後面怎麼排。
5. `PHASE_REVIEW_WORKFLOW.md`：最後看章節怎麼進入實作與審核。

## 文件規則

- 流程圖只能用純文字表示。
- 流程圖一律放在 fenced `text` blocks。
- 不要使用 Mermaid。
- 不要用 Markdown tables 描述生命周期或流程。
- 不要使用圖片或外部圖檔。
- 每一條正式流程都要顯示生產方、消費方、事件或命令、狀態變化、持久化邊界與對帳點。

## 讀完這頁之後

你應該可以知道每個資料夾是什麼、該先看哪裡，以及哪一些內容是正式章節，哪一些只是歷史參考。
