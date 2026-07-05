# LumiX

`LumiX` 是交易所專案代號，名稱來自 `Lumi` 與 `X`。

`Lumi` 帶有光、照亮與清晰可見的意象，`X` 代表 exchange。

LumiX is being organized toward a full operating OL exchange, not an MVP.

## Current Status

- Phase 11 completed as a documentation-only production architecture reset.
- Phase 12 through Phase 36 are planned and not started.
- The next implementation phase is Phase 12 - Production Database Schema & Migration.
- Production trading is not completed.
- Production launch readiness is not completed.

## Source of Truth

- `docs/README.md`
- `docs/OPERATING_EXCHANGE_MASTER_PLAN.md`
- `docs/ARCHITECTURE_TEXT_MAP.md`
- `docs/PHASE_REVIEW_WORKFLOW.md`

## Boundaries

- `web/` is the frontend.
- `server/` is the Java backend.
- Production matching must be a C++ core in `core/` or `matching-core/`.
- Java `MatchingEngineClient` is only an integration boundary.
- Do not treat stub, interface, mock, placeholder, or TODO work as production completion.
