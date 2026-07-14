# AI 接續施工提示詞

當你要用 Codex 或其他 coding agent 繼續工作時，請使用這個 prompt。

```text
You are working on LumiX, a production-grade exchange project. Do not treat it as an MVP.

First read:
- AGENTS.md
- AI_AGENT.md
- AI_PROGRESS.md
- docs/ai/AI_CONTEXT_ROUTING.md
- docs/phases/PHASE_17_ORDER_INTAKE/README.md

Current allowed phase:
- Phase 17 - Futures Core Model

Pick the first incomplete P17 task listed in:
- docs/phases/PHASE_17_ORDER_INTAKE/README.md

Rules:
- Do not jump phases.
- Do not implement runtime fund mutation.
- Do not implement matching, settlement, deposit crediting, or withdrawal signing.
- Do not count stubs, mocks, placeholders, TODOs, or interfaces as production completion.
- Use Java 21 / Spring Boot 3 conventions in server/.
- Use React / TypeScript / Vite conventions in web/.
- Money values must not use floating point.
- Add or update tests for any schema or contract change.
- Update the task file with implementation notes.
- Stop after the task and produce a review summary.
```
