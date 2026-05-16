# API curl scripts

Run from project root.

```bash
BASE_URL=http://localhost:8080 ./shells/api-curls/polymarket.sh
BASE_URL=http://localhost:8080 ./shells/api-curls/exchange-core.sh
BASE_URL=http://localhost:8080 ./shells/api-curls/all.sh
```

Default `BASE_URL` is `http://localhost:8080`.

Polymarket order/session calls use environment variables. Copy values from the API response or your wallet flow before running those sections.

Optional sections:

```bash
RUN_CLOB_AUTH=1 CLOB_AUTH_NONCE=0 ./shells/api-curls/polymarket.sh
RUN_SESSION=1 ./shells/api-curls/polymarket.sh
RUN_APPROVAL=1 OWNER=0x... ./shells/api-curls/polymarket.sh
RUN_REAL_ORDER=1 SESSION_ID=... MARKET_SLUG=... ./shells/api-curls/polymarket.sh
```

Suggested Polymarket setup flow:

1. Set `polymarket.wallet.private-key`, `polymarket.wallet.funder-address`, and `polymarket.wallet.signature-type: 3`.
2. Run `RUN_CLOB_AUTH=1 CLOB_AUTH_NONCE=0 ./shells/api-curls/polymarket.sh`.
3. Copy returned `apiKey`, `secret`, and `passphrase` into `polymarket.clob.*`.
4. Restart Spring Boot.
5. Run market sync/price refresh.
6. Run a real order only with `RUN_REAL_ORDER=1`.
