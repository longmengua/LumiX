# application/command

Use case command DTO。

目前狀態：
- `PlaceOrderCommand`、`CancelOrderCommand`、`AmendOrderCommand`、`CancelReplaceOrderCommand` 等承載 controller 轉進 use case 的輸入。
- command 應保持薄模型，不放業務邏輯。

新增規則：
- 新增 use case 時先建立 command，避免 controller 直接傳 web DTO 到 application flow。
- 欄位語意要在 record JavaDoc 裡說清楚，尤其 null 代表「沿用原值」的欄位。
