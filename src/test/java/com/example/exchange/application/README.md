# test application

Application layer tests。

目前狀態：
- 主要測試在 `service/`。
- Use case 行為多透過 service integration tests 間接覆蓋。

注意：
- 新 use case 若流程複雜，應補直接測試或在 service integration test 中覆蓋成功/失敗路徑。
