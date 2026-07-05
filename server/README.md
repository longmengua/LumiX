# LumiX Server Status

This directory currently contains the Java backend foundation and stub work from Phase 9 and Phase 10.

Implemented in this directory:

- Java 21 + Spring Boot 3 Maven project skeleton
- `account`, `ledger`, and `idempotency` contracts
- Validation-only `DefaultAccountTransferService`
- Wallet, market data, spot order, and Open API DTOs/interfaces/stubs

Not implemented in this directory:

- Production matching engine or C++ matching-core integration
- Real order persistence, real order submission, fill handling, or settlement
- Real balance mutation, reservation, release, commit, or rollback
- Double-entry ledger engine
- Real deposit or withdrawal blockchain integration
- Repository layer, controller layer, database migrations, queue consumers, websocket publication, or reconciliation jobs

Do not claim production trading from `server/`.

Authoritative production reset documents:

- `../docs/PHASE_10_AUDIT.md`
- `../docs/PRODUCTION_ROADMAP.md`
- `../docs/ARCHITECTURE_PRODUCTION.md`

Build commands:

- `./mvnw test`
- `./mvnw package`
- `./mvnw spring-boot:run`
