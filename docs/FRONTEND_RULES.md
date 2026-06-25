# LumiX Frontend Rules

## Scope

- React + TypeScript only.
- Keep feature work inside the current Phase.
- Use mock services until a later backend Phase explicitly exposes APIs.

## Hard rules

- Do not implement real matching, liquidation, margin, PnL, wallet sweep, or withdrawal execution here.
- Do not add a large UI library without explicit approval.
- Do not store real secrets in the frontend.
- Keep asset-sensitive values formatted or masked.

## UI rules

- Every page needs loading, empty, and error states when applicable.
- Keep skeleton components reusable.
- Prefer small, focused components over page-local duplication.

