# Deployment View

```text
+---------------------+          +---------------------+
| CDN / Edge          | -------> | Web App             |
+---------------------+          +---------------------+
                                          |
                                          v
+---------------------+          +---------------------+
| Load Balancer       | -------> | API Pods            |
+---------------------+          +---------------------+
                                          |
                  +-----------------------+-----------------------+
                  |                       |                       |
                  v                       v                       v
        +------------------+    +------------------+    +------------------+
        | Worker Pods      |    | PostgreSQL       |    | Redis            |
        +------------------+    +------------------+    +------------------+
                  |
                  v
        +------------------+    +------------------+
        | Chain Nodes      |    | Signing Boundary |
        +------------------+    +------------------+
```

## Deployment notes

- API pods are stateless.
- PostgreSQL is the critical stateful dependency.
- Redis is replaceable and must not contain unique funds truth.
- Signing boundary should be isolated from public API path.
- Chain listener should be idempotent and replayable.
