import { NavLink } from 'react-router-dom';

import { Badge } from '../../components/base/Badge';
import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { formatAmount, formatCurrency, formatPercent, formatPrice, formatTime } from '../../utils/format';
import { TradingAdapterNotice } from './TradingAdapterNotice';
import { TradingOrderBook } from './TradingOrderBook';
import { TradingOrderForm } from './TradingOrderForm';
import { TradingOpenOrdersTable, TradingPositionsTable, TradingRiskSnapshot } from './TradingTables';
import { TradingSectionNav } from './TradingSectionNav';
import { TradingTradeFeed } from './TradingTradeFeed';
import { useTradingWorkspaceMock } from './useTradingWorkspaceMock';
import type { TradingKind } from './mockTradingService';

type TradingWorkspaceProps = {
  kind: TradingKind;
  symbol: string;
};

const titles: Record<TradingKind, string> = {
  spot: 'Spot Trading',
  futures: 'Futures Trading',
  margin: 'Margin Trading',
};

export function TradingWorkspace({ kind, symbol }: TradingWorkspaceProps) {
  const { data, loading, error, reload } = useTradingWorkspaceMock(kind, symbol);

  return (
    <div className="stack trading-page">
      <PageHeader
        title={titles[kind]}
        description="Development adapter only. OL before must connect server/ Java API, C++ Core event stream, and real WebSocket."
        actions={
          <div className="hero-actions">
            <NavLink className="secondary-button" to="/markets">
              Markets
            </NavLink>
            <NavLink className="secondary-button" to="/assets">
              Assets
            </NavLink>
          </div>
        }
      />

      <TradingAdapterNotice notice={data?.adapterNotice ?? 'Development adapter only. OL before must connect server/ Java API, C++ Core event stream, and real WebSocket.'} />

      {loading ? <LoadingState title="Loading trading workspace" description="Fetching mock book, tape, and adapter snapshots..." /> : null}
      {error ? (
        <ErrorState
          title="Unable to load trading workspace"
          description={error}
          action={<button className="secondary-button" type="button" onClick={reload}>Retry</button>}
        />
      ) : null}

      {!loading && !error && data ? (
        <>
          <Card>
            <div className="trading-hero">
              <div className="trading-hero__copy">
                <p className="eyebrow">Development adapter only</p>
                <h2>{data.displayName}</h2>
                <p className="lead">{data.heroCopy}</p>

                <div className="trading-hero__badges">
                  <Badge tone="warning">No live order routing</Badge>
                  <Badge tone="neutral">Mock book</Badge>
                  <Badge tone="neutral">Mock tape</Badge>
                  <Badge tone="neutral">Mock balances</Badge>
                </div>

                <div className="trading-hero__meta">
                  <span>Symbol: {data.displaySymbol}</span>
                  <span>Updated: {formatTime(data.lastUpdated)}</span>
                </div>
              </div>

              <div className="trading-hero__price">
                <strong>{formatPrice(data.midPrice, 2)}</strong>
                <span className={data.change24h >= 0 ? 'pnl--positive' : 'pnl--negative'}>
                  {formatPercent(data.change24h)}
                </span>
              </div>
            </div>
          </Card>

          <TradingSectionNav kind={kind} baseAsset={data.baseAsset} />

          <Card title="Workspace Snapshot">
            <div className="dashboard-grid dashboard-grid--three">
              {data.metrics.map((metric) => (
                <div className="stat-card" key={metric.label}>
                  <span className="stat-card__label">{metric.label}</span>
                  <strong>{metric.value}</strong>
                  <p className="assets-metric__hint">{metric.hint}</p>
                </div>
              ))}
            </div>
          </Card>

          <div className="trading-layout">
            <div className="stack">
              <Card title="Order book">
                <TradingOrderBook bids={data.orderBook.bids} asks={data.orderBook.asks} lastPrice={data.midPrice} />
              </Card>

              <Card title="Trade feed">
                <TradingTradeFeed trades={data.trades} />
              </Card>

              <Card title="Development guardrails">
                <div className="stack trading-guardrails">
                  {data.orderHints.map((hint) => (
                    <div className="trading-guardrails__item" key={hint}>
                      <Badge tone="warning">Adapter</Badge>
                      <p>{hint}</p>
                    </div>
                  ))}
                </div>
              </Card>
            </div>

            <div className="stack">
              <Card title="Order entry">
                <TradingOrderForm kind={kind} workspace={data} />
              </Card>

              <Card title="Balances">
                <div className="trading-balances">
                  {data.balances.map((balance) => (
                    <div className="trading-balance" key={balance.asset}>
                      <div>
                        <p className="trading-balance__asset">{balance.asset}</p>
                        <p className="trading-balance__note">{balance.note}</p>
                      </div>
                      <div className="trading-balance__values">
                        <span>
                          Avail {balance.asset === 'USDT' ? formatCurrency(balance.available) : formatAmount(balance.available)}
                        </span>
                        <span>
                          Frozen {balance.asset === 'USDT' ? formatCurrency(balance.frozen) : formatAmount(balance.frozen)}
                        </span>
                        <strong>
                          Total {balance.asset === 'USDT' ? formatCurrency(balance.total) : formatAmount(balance.total)}
                        </strong>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>

              <Card title="Open orders">
                {data.openOrders.length > 0 ? <TradingOpenOrdersTable orders={data.openOrders} /> : <EmptyState title="No open orders" description="The adapter snapshot does not contain any open orders." />}
              </Card>

              {kind === 'futures' ? (
                <Card title="Positions">
                  {data.positions.length > 0 ? <TradingPositionsTable positions={data.positions} /> : <EmptyState title="No positions" description="The mock futures snapshot does not contain any positions." />}
                </Card>
              ) : null}

              {kind === 'margin' ? (
                <Card title="Risk snapshot">
                  <TradingRiskSnapshot riskRatio={data.riskRatio} fundingOrBorrow={data.fundingOrBorrow} />
                </Card>
              ) : null}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
