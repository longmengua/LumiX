import { useI18n } from '../../i18n';
import { formatAmount, formatPrice } from '../../utils/format';
import type { TradingOpenOrder } from './mockTradingService';

type TradingOpenOrdersPanelProps = {
  orders: TradingOpenOrder[];
};

const statusKeys = {
  Working: 'trading.order.status.working',
  'Partially Filled': 'trading.order.status.partiallyFilled',
  Queued: 'trading.order.status.queued',
} as const;

export function TradingOpenOrdersPanel({ orders }: TradingOpenOrdersPanelProps) {
  const { t } = useI18n();

  return (
    <section className="workspace-panel open-orders-panel">
      <div className="workspace-panel__header">
        <h2>{t('trading.openOrdersTitle')}</h2>
        <span>{t('trading.order.adapterPreview')}</span>
      </div>
      {orders.length === 0 ? (
        <p className="open-orders-panel__empty">{t('trading.noOpenOrdersDescription')}</p>
      ) : (
        <div className="open-orders-panel__list">
          {orders.map((order) => (
            <article className="open-orders-panel__row" key={order.id}>
              <div>
                <strong>{order.id}</strong>
                <span className={order.side === 'Buy' ? 'pnl--positive' : 'pnl--negative'}>{t(order.side === 'Buy' ? 'trading.order.side.buy' : 'trading.order.side.sell')} · {t(`trading.order.type.${order.type.toLowerCase()}`)}</span>
              </div>
              <span>{t('trading.table.price')}<strong>{formatPrice(order.price, 2)}</strong></span>
              <span>{t('trading.table.size')}<strong>{formatAmount(order.size)}</strong></span>
              <span>{t(statusKeys[order.status])}</span>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
