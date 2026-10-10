# Error Policy

## Error classes

```text
VALIDATION_ERROR      user input invalid
AUTHENTICATION_ERROR  login/session/token invalid
AUTHORIZATION_ERROR   user lacks permission
IDEMPOTENCY_CONFLICT  duplicate key with different payload
INSUFFICIENT_FUNDS    available balance not enough
RISK_REJECTED         risk policy rejected action
STATE_CONFLICT        command not valid for current state
SYSTEM_ERROR          unexpected internal failure
```

## Rule

Errors must not leak secrets, private keys, internal stack traces, SQL text, or risk bypass details.
