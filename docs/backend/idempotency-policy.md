# Idempotency Policy

## Idempotency table purpose

```text
same key + same payload   -> return same result
same key + diff payload   -> reject conflict
new key                   -> execute once
```

## Suggested fields

```text
id
scope
idempotency_key
payload_hash
status
response_code
response_body
locked_until
created_at
updated_at
```

## Scope examples

```text
user:{user_id}:order-place
user:{user_id}:withdrawal-request
system:deposit-credit
admin:{admin_id}:manual-action
```
