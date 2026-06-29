import { useEffect, useState, type FormEvent } from 'react';

import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPrice } from '../../utils/format';
import type { TradingKind, TradingWorkspaceData } from './mockTradingService';

type TradingOrderFormProps = {
  kind: TradingKind;
  workspace: TradingWorkspaceData;
};

type OrderSide = 'Buy' | 'Sell';
type OrderType = 'Limit' | 'Market';

export function TradingOrderForm({ kind, workspace }: TradingOrderFormProps) {
  const { t } = useI18n();
  const [side, setSide] = useState<OrderSide>('Buy');
  const [orderType, setOrderType] = useState<OrderType>('Limit');
  const [price, setPrice] = useState(String(workspace.orderBook.bids[0]?.price ?? 0));
  const [quantity, setQuantity] = useState('0.25');
  const [leverage, setLeverage] = useState(kind === 'futures' ? '5' : '1');
  const [messageKey, setMessageKey] = useState<string | null>(null);

  useEffect(() => {
    setPrice(String(workspace.orderBook.bids[0]?.price ?? 0));
    setQuantity('0.25');
    setLeverage(kind === 'futures' ? '5' : '1');
    setMessageKey(null);
  }, [kind, workspace]);

  const priceValue = Number(price) || workspace.orderBook.bids[0]?.price || 0;
  const quantityValue = Number(quantity) || 0;
  const notional = priceValue * quantityValue;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessageKey('trading.order.savedLocally');
  }

  return (
    <form className="trading-form" onSubmit={handleSubmit}>
      <div className="trading-form__toolbar">
        <button
          className={`tab-button${side === 'Buy' ? ' tab-button--active' : ''}`}
          type="button"
          onClick={() => setSide('Buy')}
        >
          {t('trading.order.side.buy')}
        </button>
        <button
          className={`tab-button${side === 'Sell' ? ' tab-button--active' : ''}`}
          type="button"
          onClick={() => setSide('Sell')}
        >
          {t('trading.order.side.sell')}
        </button>
        <Badge tone="warning">{t('trading.order.adapterPreview')}</Badge>
      </div>

      <div className="trading-form__grid">
        <label className="field">
          <span className="field__label">{t('trading.order.orderType')}</span>
          <select className="input" value={orderType} onChange={(event) => setOrderType(event.target.value as OrderType)}>
            <option value="Limit">{t('trading.order.type.limit')}</option>
            <option value="Market">{t('trading.order.type.market')}</option>
          </select>
        </label>

        {kind === 'futures' ? (
          <label className="field">
            <span className="field__label">{t('trading.order.leverage')}</span>
            <select className="input" value={leverage} onChange={(event) => setLeverage(event.target.value)}>
              {['2', '3', '5', '8', '10'].map((item) => (
                <option key={item} value={item}>
                  {item}x
                </option>
              ))}
            </select>
          </label>
        ) : null}

        <label className="field">
          <span className="field__label">{t('trading.order.price')}</span>
          <input className="input" inputMode="decimal" value={price} onChange={(event) => setPrice(event.target.value)} />
        </label>

        <label className="field">
          <span className="field__label">{t('trading.order.quantity')}</span>
          <input className="input" inputMode="decimal" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
        </label>
      </div>

      <div className="trading-form__summary">
        <div className="trading-form__summary-row">
          <span>{t('trading.order.side')}</span>
          <strong>{t(side === 'Buy' ? 'trading.order.side.buy' : 'trading.order.side.sell')}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>{t('trading.order.orderType')}</span>
          <strong>{t(orderType === 'Limit' ? 'trading.order.type.limit' : 'trading.order.type.market')}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>{t('trading.order.previewNotional')}</span>
          <strong>{formatCurrency(notional)}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>{t('trading.order.approxSize')}</span>
          <strong>{formatAmount(quantityValue)} {workspace.baseAsset}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>{t('trading.order.previewPrice')}</span>
          <strong>{formatPrice(priceValue, 2)}</strong>
        </div>
      </div>

      <p className="trading-form__hint">
        {t(workspace.actionLabelKey)}. {t(workspace.fundingOrBorrowKey, undefined, workspace.fundingOrBorrowValues)}
      </p>
      <p className="trading-form__hint">
        {t('trading.order.availableMockBalance')} {workspace.balances[0] ? formatCurrency(workspace.balances[0].available) : '$0.00'}
      </p>

      {messageKey ? <p className="form-message form-message--success">{t(messageKey)}</p> : null}

      <button className="primary-button" type="submit">
        {t(workspace.actionLabelKey)}
      </button>
    </form>
  );
}
