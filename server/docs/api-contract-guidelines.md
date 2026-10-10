# API Contract Guidelines

## General API response

```text
success response:
  request_id
  data
  server_time

error response:
  request_id
  error_code
  message
  details
```

## Idempotent command headers

```text
Idempotency-Key: required for effectful private commands
X-Request-Id: recommended for tracing
```

## Effectful commands

Require idempotency:

```text
place order
cancel order
withdrawal request
deposit credit internal command
admin action
manual reconciliation adjustment
```
