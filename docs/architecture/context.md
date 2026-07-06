# 系統情境

```text
+-------------------+                  +-------------------+
| Retail user       |                  | Operator          |
+-------------------+                  +-------------------+
          |                                      |
          v                                      v
+-------------------+                  +-------------------+
| LumiX Web         |                  | Admin Console     |
+-------------------+                  +-------------------+
          |                                      |
          +------------------+-------------------+
                             |
                             v
                    +-------------------+
                    | LumiX Backend     |
                    +-------------------+
                             |
          +------------------+------------------+
          |                  |                  |
          v                  v                  v
+-------------------+ +-------------------+ +-------------------+
| Database          | | Event / Workers   | | External Chains   |
+-------------------+ +-------------------+ +-------------------+
```

## 外部角色

- Retail users create accounts, deposit, trade, and withdraw.
- Operators manage markets, risk, incidents, and reconciliation.
- External chains provide trans動作 observations and settlement networks.
- Future custody providers may own signing boundaries.
