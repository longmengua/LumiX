<!-- 檔案用途：live position SQL mirror/index 決策。英文版本位於 ../en/live-position-sql-mirror.md。 -->
# Live Position SQL Mirror

本文件關閉 P1 live-position SQL mirror/index 的設計決策。

## 決策

使用專用的 durable live-position projection，不使用 `account_risk_snapshots` 取代 live-position mirror。

原因：
- Redis `pos:{uid}` 與 `pos:open:index` 目前仍是 individual positions 的低延遲 hot state。
- `account_risk_snapshots` 保存的是帳戶級 aggregates，例如 margin、equity、risk ratio 與 open-position count，不保存單一 symbol 的 quantity、entry price、realized PnL 或 side。
- Liquidation scanning、ADL、funding 與 market-maker exposure 都需要 per-position rows 與 deterministic open-position indexes。

## 已落地資料表

Migration：

```text
V23__position_lifecycle_projection.sql
```

資料表：

```text
position_lifecycle_projection
```

必要欄位：

| 欄位 | 用途 |
| --- | --- |
| `uid` | 使用者 id。 |
| `symbol` | 交易 symbol。 |
| `mode` | Margin mode，例如 `CROSS` 或 `ISOLATED`。 |
| `leverage` | 目前槓桿。 |
| `qty` | 帶正負號的 position quantity；零代表已平倉。 |
| `entry_price` | 加權平均進場價。 |
| `margin` | isolated margin 或 cross-position margin allocation。 |
| `realized_pnl` | 累積 realized PnL。 |
| `fee_paid` / `rebate_earned` | 累積 fee 與 rebate。 |
| `funding_paid` / `funding_received` | 累積 funding 欄位。 |
| `insurance_fund_covered` / `adl_covered` | Liquidation shortfall coverage 欄位。 |
| `last_trade_ref` | 最近一次改動 position 的 internal trade、match、liquidation、funding 或 ADL reference。 |
| `updated_at` | 最新 projection update time。 |

Primary key：

```text
(uid, symbol)
```

必要 indexes：

| Index | 查詢 |
| --- | --- |
| `(symbol, qty, updated_at)` | 依 symbol 做 liquidation / ADL / open-position scans。 |
| `(uid, updated_at)` | 使用者 position 畫面與 account restore checks。 |
| `(updated_at)` | 營運 drift 與 stale-projection scans。 |

只有在 MySQL production cardinality 下 `qty <> 0` query plan 不理想時，才加 generated `is_open` boolean。

## 重建來源

Projection 必須可由 durable state 重建：

1. Matching trade events，用於 fills 與 signed quantity movement。
2. Wallet ledger journal rows，用於 fees、rebates、funding、liquidation shortfall、insurance 與 ADL coverage。
3. Durable ADL / liquidation execution records，用於 command idempotency 與 operator audit references。
4. Account risk snapshots 只作 validation evidence，不作 per-position rebuild source。

Redis 修復規則：

1. 先重建 `position_lifecycle_projection`。
2. 再從非零 SQL projection rows 重建 Redis `pos:{uid}` 與 `pos:open:index`。
3. 不要為了修 Redis drift 而重跑原始 trade / liquidation / funding command。

## 已完成 Baseline

本 slice 已新增：

- `position_lifecycle_projection` 的 Flyway migration。
- 支援 symbol open-position scan、user position read、stale projection scan 的 production query indexes。
- JPA entity/repository baseline：`PositionLifecycleProjection` 與 `PositionLifecycleProjectionJpaRepository`。

## 剩餘 Implementation Gate

後續 implementation slice 應新增：

- 由 trade/funding/liquidation/ADL state changes 更新 projection 的路徑。
- 比對 Redis open positions 與 SQL projection rows 的 rebuild / reconciliation service。
- 覆蓋 open-position scan、zero-quantity close/removal、Redis rebuild source 與 account-position consistency 的 focused tests。

在 update/rebuild wiring 落地前，Redis 仍是 serving hot state，SQL mirror 則是 durable schema/query baseline。
