# Codex Project Kit

This package is a portable startup kit for new Codex-assisted projects.

Copy this `README.md` and the `doc/` directory into a new repository root, then run:

```bash
bash doc/install.sh
```

The installer creates:

- `AGENTS.md` from `doc/AGENTS.md`
- `shells/codex-usage.sh` from `doc/scripts/codex-usage.sh`

After that, Codex will automatically read `AGENTS.md` when it starts in the project.

## Daily Workflow

Start a task:

```bash
./shells/codex-usage.sh start task-name
```

Finish a task:

```bash
./shells/codex-usage.sh end task-name
```

Current usage:

```bash
./shells/codex-usage.sh status
```

## Recommended Project Layout

```text
.
├── AGENTS.md
├── README.md
├── doc/
│   ├── AGENTS.md
│   ├── ai/
│   │   ├── project-structure.md
│   │   ├── team-collaboration.md
│   │   └── team-management.md
│   ├── tasks/
│   │   ├── README.md
│   │   └── active.md
│   ├── scripts/
│   │   └── codex-usage.sh
│   └── install.sh
└── shells/
    └── codex-usage.sh
```

## What This Kit Standardizes

- One-writer-per-worktree collaboration.
- AI-readable project structure and naming rules for new code.
- Claim-before-code workflow for parallel agents.
- Focused task files with goal, scope, acceptance criteria, and read-first links.
- Automatic local Codex usage reporting from `~/.codex/sessions`.
- Final reports with commit, tests, remaining TODOs, and usage delta.
