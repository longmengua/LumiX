import { useState } from 'react';

import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPercent, formatPrice } from '../../utils/format';
import type { TradingBalance, TradingOpenOrder, TradingPosition } from './mockTradingService';

type TradingBalancePanelProps = {
  balances: TradingBalance[];
  positions: TradingPosition[];
  openOrders: TradingOpenOrder[];
  riskRatio: number;
};

const orderStatusKeys = {
  Working: 'trading.order.status.working',
  'Partially Filled': 'trading.order.status.partiallyFilled',
  Queued: 'trading.order.status.queued',
} as const;

export function TradingBalancePanel({ balances, positions, openOrders, riskRatio }: TradingBalancePanelProps) {
  const { t } = useI18n();
  const [tab, setTab] = useState<'assets' | 'positions' | 'orders'>('assets');
  const quoteBalance = balances.find((balance) => balance.asset === 'USDT') ?? balances[0];
  const unrealizedPnl = positions.reduce((sum, position) => sum + position.pnl, 0);
  const maintenanceMargin = positions.reduce((sum, position) => sum + position.marginUsed * 0.005, 0);

  return (
    <section className="workspace-panel balance-panel">
      <div className="workspace-panel__header">
        <h2>{t('trading.balances.title')}</h2>
        <span>{t('trading.order.adapterPreview')}</span>
      </div>
      <div className="balance-panel__tabs" role="tablist" aria-label={t('trading.balances.title')}>
        <button className={tab === 'assets' ? 'balance-panel__tab balance-panel__tab--active' : 'balance-panel__tab'} type="button" onClick={() => setTab('assets')}>
          {t('trading.balance.tabs.assets')}
        </button>
        <button className={tab === 'positions' ? 'balance-panel__tab balance-panel__tab--active' : 'balance-panel__tab'} type="button" onClick={() => setTab('positions')}>
          {t('trading.balance.tabs.positions')}
        </button>
        <button className={tab === 'orders' ? 'balance-panel__tab balance-panel__tab--active' : 'balance-panel__tab'} type="button" onClick={() => setTab('orders')}>
          {t('trading.openOrdersTitle')}
        </button>
      </div>

      {tab === 'assets' ? (
        <>
          <div className="balance-panel__metrics">
            <span>{t('trading.balance.availableMargin')}<strong>{quoteBalance ? formatCurrency(quoteBalance.available) : formatCurrency(0)}</strong></span>
            <span>{t('trading.balance.walletBalance')}<strong>{quoteBalance ? formatCurrency(quoteBalance.total) : formatCurrency(0)}</strong></span>
            <span>{t('trading.balance.maintenanceMargin')}<strong>{formatCurrency(maintenanceMargin)}</strong></span>
            <span>{t('trading.balance.unrealizedPnl')}<strong className={unrealizedPnl >= 0 ? 'pnl--positive' : 'pnl--negative'}>{formatCurrency(unrealizedPnl)}</strong></span>
            <span>{t('trading.balance.marginRatio')}<strong>{formatPercent(riskRatio)}</strong></span>
          </div>
          <div className="balance-panel__assets">
            {balances.map((balance) => (
              <div key={balance.asset}>
                <span>{balance.asset}</span>
                <strong>{balance.asset === 'USDT' ? formatCurrency(balance.available) : formatAmount(balance.available)}</strong>
              </div>
            ))}
          </div>
        </>
      ) : tab === 'positions' ? (
        <div className="balance-panel__positions">
          {positions.length === 0 ? <p className="balance-panel__empty">{t('trading.noPositionsDescription')}</p> : positions.map((position) => (
            <article className="balance-panel__position" key={`${position.symbol}-${position.side}`}>
              <div>
                <strong>{position.symbol}</strong>
                <span className={position.side === 'Long' ? 'pnl--positive' : 'pnl--negative'}>{t(position.side === 'Long' ? 'trading.position.side.long' : 'trading.position.side.short')} {position.leverage}x</span>
              </div>
              <span>{t('trading.table.size')}<strong>{formatAmount(position.size)}</strong></span>
              <span>{t('trading.table.mark')}<strong>{formatPrice(position.markPrice, 2)}</strong></span>
              <span>{t('trading.table.pnl')}<strong className={position.pnl >= 0 ? 'pnl--positive' : 'pnl--negative'}>{formatCurrency(position.pnl)}</strong></span>
            </article>
          ))}
        </div>
      ) : (
        <div className="balance-panel__orders">
          {openOrders.length === 0 ? <p className="balance-panel__empty">{t('trading.noOpenOrdersDescription')}</p> : openOrders.map((order) => (
            <article className="balance-panel__order" key={order.id}>
              <div>
                <strong>{order.id}</strong>
                <span className={order.side === 'Buy' ? 'pnl--positive' : 'pnl--negative'}>{t(order.side === 'Buy' ? 'trading.order.side.buy' : 'trading.order.side.sell')} · {t(`trading.order.type.${order.type.toLowerCase()}`)}</span>
              </div>
              <span>{t('trading.table.price')}<strong>{formatPrice(order.price, 2)}</strong></span>
              <span>{t('trading.table.size')}<strong>{formatAmount(order.size)}</strong></span>
              <span>{t(orderStatusKeys[order.status])}</span>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
