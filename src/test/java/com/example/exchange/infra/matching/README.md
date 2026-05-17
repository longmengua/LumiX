# test infra/matching

In-memory matching engine tests。

目前內容：
- LIMIT/MARKET matching。
- IOC/FOK/post-only/self-match 行為。
- cancel/amend/snapshot 行為。

注意：
- matching engine 仍是 MVP in-memory core；新增撮合規則時要先補這裡的 deterministic tests。
