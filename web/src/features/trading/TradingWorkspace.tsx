import { NavLink } from 'react-router-dom';

import { Badge } from '../../components/base/Badge';
import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
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
  spot: 'trading.spot.title',
  futures: 'trading.futures.title',
  margin: 'trading.margin.title',
};

const badgeKeys = {
  noLiveRouting: 'trading.badge.noLiveRouting',
  mockBook: 'trading.badge.mockBook',
  mockTape: 'trading.badge.mockTape',
  mockBalances: 'trading.badge.mockBalances',
} as const;

export function TradingWorkspace({ kind, symbol }: TradingWorkspaceProps) {
  const { t } = useI18n();
  const { data, loading, error, reload } = useTradingWorkspaceMock(kind, symbol);

  return (
    <div className="stack trading-page trading-page--workspace">
      <PageHeader
        title={t(titles[kind])}
        description={t('trading.developmentNotice.description')}
        actions={
          <div className="hero-actions">
            <NavLink className="secondary-button" to="/markets">
              {t('trading.marketShortcut')}
            </NavLink>
            <NavLink className="secondary-button" to="/assets">
              {t('trading.assetsShortcut')}
            </NavLink>
          </div>
        }
      />

      <TradingAdapterNotice notice={data?.adapterNoticeKey ? t(data.adapterNoticeKey) : t('trading.developmentNotice.description')} />

      {loading ? <LoadingState title={t('trading.loadingTitle')} description={t('trading.loadingDescription')} /> : null}
      {error ? (
        <ErrorState
          title={t('trading.errorTitle')}
          description={error}
          action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>}
        />
      ) : null}

      {!loading && !error && data ? (
        <>
          <Card>
            <div className="trading-hero">
              <div className="trading-hero__copy">
                <p className="eyebrow">{t('trading.developmentNotice.title')}</p>
                <h2>{t(data.displayNameKey, undefined, data.displayNameValues)}</h2>
                <p className="lead">{t(data.heroCopyKey, undefined, data.heroCopyValues)}</p>

                <div className="trading-hero__badges">
                  <Badge tone="warning">{t(badgeKeys.noLiveRouting)}</Badge>
                  <Badge tone="neutral">{t(badgeKeys.mockBook)}</Badge>
                  <Badge tone="neutral">{t(badgeKeys.mockTape)}</Badge>
                  <Badge tone="neutral">{t(badgeKeys.mockBalances)}</Badge>
                </div>

                <div className="trading-hero__meta">
                  <span>{t('trading.instrument.symbol')}: {data.displaySymbol}</span>
                  <span>{t('trading.instrument.updatedAt')}: {formatTime(data.lastUpdated)}</span>
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

          <Card title={t('trading.snapshot.title')}>
            <div className="dashboard-grid dashboard-grid--three">
              {data.metrics.map((metric) => (
                <div className="stat-card" key={metric.labelKey}>
                  <span className="stat-card__label">{t(metric.labelKey)}</span>
                  <strong>{metric.value}</strong>
                  <p className="assets-metric__hint">{t(metric.hintKey)}</p>
                </div>
              ))}
            </div>
          </Card>

          <div className="trading-layout">
            <div className="stack">
              <Card title={t('trading.orderBook.title')}>
                <TradingOrderBook bids={data.orderBook.bids} asks={data.orderBook.asks} lastPrice={data.midPrice} />
              </Card>

              <Card title={t('trading.recentTrades.title')}>
                <TradingTradeFeed trades={data.trades} />
              </Card>

              <Card title={t('trading.guardrailsTitle')}>
                <div className="stack trading-guardrails">
                  {data.orderHintCopies.map((hint) => (
                    <div className="trading-guardrails__item" key={hint.key}>
                      <Badge tone="warning">{t('trading.adapterLabel')}</Badge>
                      <p>{t(hint.key, undefined, hint.values)}</p>
                    </div>
                  ))}
                </div>
              </Card>
            </div>

            <div className="stack">
              <Card title={t('trading.orderEntry.title')}>
                <TradingOrderForm kind={kind} workspace={data} />
              </Card>

              <Card title={t('trading.balances.title')}>
                <div className="trading-balances">
                  {data.balances.map((balance) => (
                    <div className="trading-balance" key={balance.asset}>
                      <div>
                        <p className="trading-balance__asset">{balance.asset}</p>
                        <p className="trading-balance__note">{t(balance.noteKey)}</p>
                      </div>
                      <div className="trading-balance__values">
                        <span>
                          {t('trading.balance.available')} {balance.asset === 'USDT' ? formatCurrency(balance.available) : formatAmount(balance.available)}
                        </span>
                        <span>
                          {t('trading.balance.frozen')} {balance.asset === 'USDT' ? formatCurrency(balance.frozen) : formatAmount(balance.frozen)}
                        </span>
                        <strong>
                          {t('trading.balance.total')} {balance.asset === 'USDT' ? formatCurrency(balance.total) : formatAmount(balance.total)}
                        </strong>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>

              <Card title={t('trading.openOrdersTitle')}>
                {data.openOrders.length > 0 ? <TradingOpenOrdersTable orders={data.openOrders} /> : <EmptyState title={t('trading.noOpenOrdersTitle')} description={t('trading.noOpenOrdersDescription')} />}
              </Card>

              {kind === 'futures' ? (
                <Card title={t('trading.positions.title')}>
                  {data.positions.length > 0 ? <TradingPositionsTable positions={data.positions} /> : <EmptyState title={t('trading.noPositionsTitle')} description={t('trading.noPositionsDescription')} />}
                </Card>
              ) : null}

              {kind === 'margin' ? (
                <Card title={t('trading.risk.title')}>
                  <TradingRiskSnapshot riskRatio={data.riskRatio} fundingOrBorrow={t(data.fundingOrBorrowKey, undefined, data.fundingOrBorrowValues)} />
                </Card>
              ) : null}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
