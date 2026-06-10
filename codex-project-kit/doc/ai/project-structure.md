# Project Structure and Naming

This file is the AI-readable source of truth for where new code and docs should go.
Keep it short, update it when introducing a new top-level folder, package family, file suffix, or ownership boundary.

## Core Rule

Follow the repository's existing architecture before creating a new pattern.

When adding a new file:

1. Put it in the narrowest existing folder that owns the behavior.
2. Use the existing suffix and naming style for that folder.
3. Update this file when the new code introduces a new folder, role, suffix, or cross-cutting convention.
4. Update the relevant code map or task docs when the change affects ownership, status, or common entry points.

Do not create a new top-level folder, package family, or suffix unless it removes real ambiguity or matches a repeated project need.

## Common Layout

Recommended baseline for application repositories:

```text
.
├── AGENTS.md
├── README.md
├── doc/
│   ├── ai/
│   │   ├── project-structure.md
│   │   ├── team-collaboration.md
│   │   └── team-management.md
│   └── tasks/
├── shells/
└── src/
```

Use project-specific names when the repository already has them, such as `docs/` instead of `doc/`.
Prefer documenting the local convention over renaming a working repository.

## Layering Pattern

For layered backend services, keep dependencies flowing inward:

```text
interface adapters
  -> application workflows
  -> domain contracts and rules
  -> infrastructure adapters
```

Suggested ownership:

- `interfaces` or `adapters/inbound`: REST, WebSocket, CLI, consumers, DTOs, request validation, authentication entry points.
- `application`: use cases, commands, schedulers, orchestration services, transaction boundaries.
- `domain`: entities, value objects, domain events, repository contracts, pure domain services, domain utilities.
- `infra` or `adapters/outbound`: database, cache, queue, HTTP clients, metrics, tracing, storage, third-party integrations.
- `resources` or `config`: runtime configuration, migrations, static assets, templates.
- `test`: tests that mirror production package ownership where practical.

Interface adapters should translate protocols and enforce access rules.
Application code should coordinate workflows.
Domain code should hold business rules without depending on framework or transport details.
Infrastructure code should contain concrete platform and vendor integrations.

## Naming Pattern

Use names that identify the role of the file without opening it:

- Inbound API entry points: `*Controller`, `*Consumer`, `*Handler`, `*Endpoint`.
- Request/response models: `*Request`, `*Response`.
- Application inputs: `*Command`, `*Query`.
- Use-case entry points: `*UseCase`.
- Shared workflow services: `*Service`.
- Scheduled jobs: `*Scheduler`, `*Job`.
- Domain events: past-tense business facts, such as `OrderPlaced` or `TradeExecuted`.
- Repository contracts: `*Repository`, `*Store`, `*Log`, or `*Journal`, matching local convention.
- Concrete adapters: prefix or suffix with the technology, such as `Jpa*Store`, `Redis*Repository`, `Kafka*Publisher`, `Http*Client`.
- Configuration: `*Config`, `*Properties`.
- Tests: mirror the target class and add `*Test`.
- Database migrations: use the migration tool's native ordering, such as Flyway `V{n}__snake_case_description.sql`.

Avoid near-miss names that differ only by typo or vague wording. For example, use `OrderUseCase`, not `OrderUserCase`.

## Documentation Updates

When adding a new behavior area, update the smallest useful AI-readable document:

- Add or update a code map when agents need a reliable entry point for the area.
- Add or update a task file when the work becomes an executable lane.
- Update this file when the change creates a reusable structural or naming rule.
- Update `AGENTS.md` only when the rule affects agent startup behavior.

Do not duplicate long design explanations across files. Link to the primary doc instead.

## Comment Policy

Every new or modified code artifact must include useful comments. This includes application code, tests, SQL migrations, scripts, and frontend code.

Comments should explain why the code exists and what operational or business rule it protects:

- Application code: intent, state transitions, accounting effects, security decisions, idempotency, replay, and recovery behavior.
- Tests: scenario, setup, action, and expected result.
- SQL migrations: table ownership, non-obvious columns, indexes, constraints, and operational assumptions.
- Frontend code: user workflow, API contract assumptions, auth/session handling, and error states.

Avoid comments that only restate syntax.
