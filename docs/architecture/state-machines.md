# 狀態機

## 訂單 state machine

```text
+---------+
| NEW     |
+----+----+
     |
     v
+----+----+       reject        +----------+
| ACCEPTED | ----------------> | REJECTED |
+----+----+                    +----------+
     |
     +------ partial fill ----+
     |                        v
     |                  +-----+------+
     |                  | PART_FILLED|
     |                  +-----+------+
     |                        |
     |                        +---- fill remaining ----+
     |                        |                        v
     v                        |                  +-----+----+
+----+----+                   +----------------> | FILLED   |
| OPEN    |                                      +----------+
+----+----+
     |
     +---- cancel ----> +-----------+
                        | CANCELLED |
                        +-----------+
```

## 預留 state machine

```text
+-----------+
| REQUESTED |
+-----+-----+
      |
      v
+-----+-----+      fail       +--------+
| HELD      | --------------> | FAILED |
+--+-----+--+                 +--------+
   |     |
   |     +---- release ----> +----------+
   |                         | RELEASED |
   |                         +----------+
   |
   +---- capture ----------> +----------+
                             | CAPTURED |
                             +----------+
```

## 提款al state machine

```text
REQUESTED -> RISK_REVIEW -> APPROVED -> SIGNING -> BROADCASTED -> CONFIRMED
     |            |             |          |            |
     v            v             v          v            v
  REJECTED     REJECTED      CANCELLED   FAILED       FAILED
```
