#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="src/test/java"

if [ ! -d "$TEST_DIR" ]; then
  echo "No src/test/java found"
  exit 0
fi

echo "==> Fix test imports from domain.model.entity to domain.model.dto"

# 只改測試裡面已經搬到 dto 的類
DTO_CLASSES=(
  Account
  DlqEvent
  Order
  OutboxEvent
  Position
  Symbol
  SymbolConfig
  WalletLedgerEntry
  WalletLedgerPosting
  WalletTransfer
)

for cls in "${DTO_CLASSES[@]}"; do
  find "$TEST_DIR" -name "*.java" -type f -print0 \
    | xargs -0 perl -0pi -e "s/import com\\.example\\.exchange\\.domain\\.model\\.entity\\.${cls};/import com.example.exchange.domain.model.dto.${cls};/g"
done

echo "==> Check remaining old imports in tests"
grep -R "import com.example.exchange.domain.model.entity" "$TEST_DIR" || true

echo "==> Done"
echo "Next: ./mvnw test"
