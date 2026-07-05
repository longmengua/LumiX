# Phase Command Workflow

1. Receive one phase command.
2. Reload the repo from disk.
3. Read the master plan and review workflow.
4. Read the current phase spec under `docs/phases/`.
5. Implement only that phase.
6. Validate build and tests.
7. Mark `implementation completed / pending human review`.
8. Wait for explicit human approval.
9. Only then mark the phase completed.
10. Do not start the next phase automatically.

