# 事故應對

## Incident flow

```text
Alert -> Triage -> Contain -> Communicate -> Repair -> Reconcile -> Postmortem
```

## Containment 動作s

```text
pause market
pause withdrawals
pause deposit crediting
increase confirmation threshold
disable risky admin action
switch service to read-only mode
```

## Postmortem required fields

```text
summary
impact
timeline
root cause
what stopped the incident
what failed
actions
owner
due date
```
