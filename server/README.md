# LumiX Server Phase 9

This directory contains the Phase 9 backend foundation for LumiX.

Scope in this phase:
- Java 21 + Spring Boot 3 Maven project skeleton only
- Backend code lives only in `server/`
- `account`, `ledger`, and `idempotency` boundaries and interfaces
- Account transfer validation stub only

Out of scope in this phase:
- Production-ready ledger engine
- Real balance mutation
- Wallet deposit or withdrawal logic
- Market data, price index, spot order, or Open API services
- Any Phase 10 or later functionality

Notes:
- High-risk logic is intentionally left as interface or stub with explicit TODO markers.
- This phase does not claim production readiness.
- A global Maven installation is not required; use Maven Wrapper in `server/`.
- To build locally, use Java 21 with the wrapper commands below.

Build commands:
- `./mvnw test`
- `./mvnw package`
- `./mvnw spring-boot:run`
