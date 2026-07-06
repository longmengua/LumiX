# State Management

## Rule

Client state is not funds truth.

```text
Server balance projection -> frontend display
Frontend form estimate    -> preview only
Ledger truth              -> backend only
```

## Money display

- Do not use JavaScript floating point for final money calculation.
- Use string decimal values from API.
- Show asset precision consistently.
- Clearly separate available, held, pending withdrawal, and total.
