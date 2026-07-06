# 部署運行手冊

## Deployment sequence

```text
1. Check migration status
2. Run backward-compatible migrations
3. Deploy backend
4. Deploy workers
5. Deploy frontend
6. Verify health checks
7. Verify smoke tests
8. Watch metrics and logs
```

## Rollback rule

Application rollback must not assume database rollback is possible. Schema migrations should be backward compatible whenever possible.
