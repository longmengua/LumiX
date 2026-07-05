# Architecture Text Map

## Full Operating Exchange Architecture

```text
                                  +----------------------+
                                  |   Open API Clients   |
                                  +----------+-----------+
                                             |
                                             | sync request path
                                             v
+-------------------------+      +-------------------------------+      +--------------------------+
|      Web Frontend       | ---> | Java Spring Boot Backend      | ---> |  Production services     |
|                         |      |                               |      |                          |
| public market pages     |      | REST controllers              |      | account                  |
| trading UI              |      | WebSocket / SSE               |      | wallet                   |
| wallet UI               |      | auth / API key                |      | spot order               |
| account / KYC placeholder|     | risk                          |      | ledger                   |
| admin UI                |      | admin                         |      | reservation / freeze     |
|                         |      | reconciliation                |      | settlement               |
|                         |      | market data                   |      | market data              |
|                         |      +---------------+---------------+      | Open API                 |
|                         |                      |                      | admin                    |
|                         |                      | async event path     | reconciliation           |
|                         |                      v                      +-----------+--------------+
|                         |      +-------------------------------+                  |
|                         |      |  C++ Matching Core (planned)  |                  |
|                         |      | order book                    |                  |
|                         |      | deterministic matching        |                  |
|                         |      | sequence / replay             |                  |
|                         |      +---------------+---------------+                  |
|                         |                      |                                   |
|                         |                      | market data path                 |
|                         |                      v                                   |
|                         |           +------------------------+                    |
|                         |           |  Market Data Pipeline   | <------------------+
|                         |           +------------------------+
|                         |
|                         +---------------- private user event path -----------------+
|                                                                                     |
|                             funds path                                              |
|                                                                                     v
|                        +---------------------------+   +--------------------------+
|                        | Ledger / Reservation      |<--| Wallet / Treasury        |
|                        | Double-entry, freeze      |   | chain scanner, hot/cold  |
|                        +---------------------------+   | signer boundary          |
|                                                         +--------------------------+
|
+-------------------------------+
| Operations                    |
| monitoring                    |
| alerts                        |
| runbooks                      |
| CI/CD                         |
| backup / restore              |
+-------------------------------+
```

## Production Trading Flow

```text
User / API client
  -> Frontend / Open API
  -> Java order service [stub / planned; production required]
  -> risk pre-check [planned]
  -> fund reservation [planned]
  -> order persistence [planned]
  -> C++ matching core [planned]
  -> fill event [planned]
  -> settlement engine [planned]
  -> ledger journal [planned]
  -> balance projection [planned]
  -> market data [planned]
  -> WebSocket / REST query [planned]
  -> reconciliation [planned]
```

Legend:

- `done`: currently implemented and verified in repo
- `stub`: interface, placeholder, or validation-only behavior
- `planned`: required production capability, not implemented yet
- `not started`: no runtime implementation exists
- `production required`: must exist before live operation

## Module Map

```text
web/
  -> pages / routes
  -> adapters / API client
  -> auth state
  -> trading views
  -> wallet views
  -> admin views

server/
  -> controller / DTO
  -> application service
  -> domain service
  -> repository
  -> infra adapter

matching-core/ or core/
  -> matching engine planned

docs/
  -> architecture / roadmap / status / runbooks / validation / legacy
```

## Runtime Truth

- Frontend pages and mock adapters exist.
- Server has a Spring Boot skeleton with stubs and interfaces.
- Production ledger, freeze, matching, settlement, deposit, withdrawal, and market-data runtime do not exist yet.
- `MatchingEngineClient` is an integration boundary, not a production engine.
- No file in this repo should claim production trading completed before the readiness gates pass.

