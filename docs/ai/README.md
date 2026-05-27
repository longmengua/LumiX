# AI Documentation

This directory contains compact, agent-facing context. It is intentionally split so an agent can read the index first and open only the relevant sub-map.

Start here:
- [code-map.md](code-map.md)

Sub-maps live in [maps/](maps/).

## How To Ask Codex

Use a Markdown file as the task entry point:

```text
讀一下 docs/en/todo.md，從最前面的未完成 P0 開始做。
```

Codex should read that file first, open the relevant map under `docs/ai/maps/`, and either start the clear next task or ask which task to start if the file leaves multiple good options.

For interrupt work, ask Codex to create a task file first:

```text
把這個插單需求轉成 task md：先做做市商對沖。
```

Then choose the generated file:

```text
讀一下 docs/tasks/core-kernel/05-market-maker-hedging.md，開始做。
```
