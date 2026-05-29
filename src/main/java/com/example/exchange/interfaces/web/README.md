# interfaces/web

Web interface 層。

目錄：
- `controller/`：REST controllers。
- `dto/`：REST request/response DTO。
- `interceptor/`：request logging、API auth、安全控制。
- `security/`：API key/JWT/IP allowlist/rate-limit helper。
- `exception/`：統一錯誤處理。
- `validator/`：Bean Validation extensions。

目前狀態：
- 內部交易所、risk、margin、market data、recovery、Polymarket API 都在這層暴露。
- WebSocket/SSE push config 位於本目錄根層；user WebSocket 支援 cancel-on-disconnect 與 `resumeConnectionId` registration transfer。
