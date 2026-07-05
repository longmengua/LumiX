# AI_CONTINUE_PROMPT_V2.md

## Continue Work Prompt

```text
Reload the repo from disk before doing anything. Read:
- AI_START_HERE.md
- AI_MODEL_GATE.md
- AI_PROGRESS.md
- docs/PRODUCTION_ROADMAP.md
- docs/PHASE_DEPENDENCY_MAP.md
- docs/PRODUCTION_READINESS_GATES.md
- docs/CODEX_PHASE_PROMPTS.md
- the current phase file in docs/phases/

Rules:
1. Do not jump phases.
2. The next implementation phase is Phase 12 until AI_PROGRESS.md says otherwise.
3. Implement only the current phase prompt from docs/CODEX_PHASE_PROMPTS.md.
4. Do not count stubs, interfaces, mocks, placeholders, or TODOs as completed production work.
5. Do not claim production trading completed until the readiness gates pass.
6. Do not claim production launch ready before Phase 36 passes with explicit human sign-off.
7. Run build and test validation after the phase work.
8. Update AI_PROGRESS.md after the phase work.
```
