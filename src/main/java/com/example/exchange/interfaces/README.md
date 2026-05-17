# interfaces

系統入口層。

目錄：
- `web/`：REST、WebSocket/SSE、security、DTO、exception、validator。
- `consumer/`：Kafka consumers。

目前狀態：
- REST API 是主要操作入口。
- Kafka consumers 主要負責事件轉推與 Polymarket user events 處理。

注意：
- interfaces 只做協議轉換與認證授權，不放核心業務規則。
- Web DTO 不應直接傳入 domain service；先轉 application command。
