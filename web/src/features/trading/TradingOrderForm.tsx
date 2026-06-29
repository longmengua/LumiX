import { useEffect, useState, type FormEvent } from 'react';

import { Badge } from '../../components/base/Badge';
import { formatAmount, formatCurrency, formatPrice } from '../../utils/format';
import type { TradingKind, TradingWorkspaceData } from './mockTradingService';

type TradingOrderFormProps = {
  kind: TradingKind;
  workspace: TradingWorkspaceData;
};

type OrderSide = 'Buy' | 'Sell';
type OrderType = 'Limit' | 'Market';

export function TradingOrderForm({ kind, workspace }: TradingOrderFormProps) {
  const [side, setSide] = useState<OrderSide>('Buy');
  const [orderType, setOrderType] = useState<OrderType>('Limit');
  const [price, setPrice] = useState(String(workspace.orderBook.bids[0]?.price ?? 0));
  const [quantity, setQuantity] = useState('0.25');
  const [leverage, setLeverage] = useState(kind === 'futures' ? '5' : '1');
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    setPrice(String(workspace.orderBook.bids[0]?.price ?? 0));
    setQuantity('0.25');
    setLeverage(kind === 'futures' ? '5' : '1');
    setMessage(null);
  }, [kind, workspace]);

  const priceValue = Number(price) || workspace.orderBook.bids[0]?.price || 0;
  const quantityValue = Number(quantity) || 0;
  const notional = priceValue * quantityValue;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage('Preview saved locally. No live order is transmitted in this development adapter.');
  }

  return (
    <form className="trading-form" onSubmit={handleSubmit}>
      <div className="trading-form__toolbar">
        <button
          className={`tab-button${side === 'Buy' ? ' tab-button--active' : ''}`}
          type="button"
          onClick={() => setSide('Buy')}
        >
          Buy
        </button>
        <button
          className={`tab-button${side === 'Sell' ? ' tab-button--active' : ''}`}
          type="button"
          onClick={() => setSide('Sell')}
        >
          Sell
        </button>
        <Badge tone="warning">Adapter preview</Badge>
      </div>

      <div className="trading-form__grid">
        <label className="field">
          <span className="field__label">Order type</span>
          <select className="input" value={orderType} onChange={(event) => setOrderType(event.target.value as OrderType)}>
            <option value="Limit">Limit</option>
            <option value="Market">Market</option>
          </select>
        </label>

        {kind === 'futures' ? (
          <label className="field">
            <span className="field__label">Leverage</span>
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
          <span className="field__label">Price</span>
          <input className="input" inputMode="decimal" value={price} onChange={(event) => setPrice(event.target.value)} />
        </label>

        <label className="field">
          <span className="field__label">Quantity</span>
          <input className="input" inputMode="decimal" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
        </label>
      </div>

      <div className="trading-form__summary">
        <div className="trading-form__summary-row">
          <span>Side</span>
          <strong>{side}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>Order type</span>
          <strong>{orderType}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>Preview notional</span>
          <strong>{formatCurrency(notional)}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>Approx. size</span>
          <strong>{formatAmount(quantityValue)} {workspace.baseAsset}</strong>
        </div>
        <div className="trading-form__summary-row">
          <span>Preview price</span>
          <strong>{formatPrice(priceValue, 2)}</strong>
        </div>
      </div>

      <p className="trading-form__hint">{workspace.actionLabel}. {workspace.fundingOrBorrow}</p>
      <p className="trading-form__hint">Available mock balance: {workspace.balances[0] ? formatCurrency(workspace.balances[0].available) : '$0.00'}</p>

      {message ? <p className="form-message form-message--success">{message}</p> : null}

      <button className="primary-button" type="submit">
        {workspace.actionLabel}
      </button>
    </form>
  );
}
