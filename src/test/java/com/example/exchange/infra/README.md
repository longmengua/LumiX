# test infra

Infrastructure tests。

目前狀態：
- `matching/` 覆蓋 in-memory matching engine、replay 與 owner epoch audit。
- `hedging/` 覆蓋 hedge venue retry、throttle 與 idempotency decorators。
- Redis/Kafka adapter 目前主要靠 application flow 與 Spring context smoke test 間接覆蓋。
