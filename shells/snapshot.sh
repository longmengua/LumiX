# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/snapshot.sh。
curl -X POST http://localhost:8080/api/recovery/snapshot \
  -H "Content-Type: application/json" \
  -d '{ "uid": 1 }'
