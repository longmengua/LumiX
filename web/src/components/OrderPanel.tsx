import { Side, TradeFormState } from '../types/exchange';
import { toNumber as parseNumber } from '../lib/exchange';
import { useI18n } from '../lib/i18n';

/**
 * Order-entry form block (UI only in this phase).
 * Keep this component isolated so submit handlers and auth gating
 * can be attached without touching layout code.
 */
interface Props {
  side: Side;
  type: 'LIMIT' | 'MARKET';
  price: string;
  qty: string;
  onStateChange: (next: TradeFormState) => void;
}

export function OrderPanel({ side, type, price, qty, onStateChange }: Props) {
  const canSubmit = Number.isFinite(parseNumber(price)) && Number.isFinite(parseNumber(qty));
  const { t } = useI18n();

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('order.title')}</h2>
      </div>
      <form className="order-form" onSubmit={(event) => event.preventDefault()}>
        <label>
          {t('order.label.side')}
          <select
            value={side}
            onChange={(event) =>
              onStateChange({
                side: (event.currentTarget.value as Side) || 'BUY',
                type,
                price,
                qty
              })
            }
          >
            <option>BUY</option>
            <option>SELL</option>
          </select>
        </label>
        <label>
          {t('order.label.type')}
          <select
            value={type}
            onChange={(event) =>
              onStateChange({
                side,
                type: (event.currentTarget.value as 'LIMIT' | 'MARKET') || 'LIMIT',
                price,
                qty
              })
            }
          >
            <option>LIMIT</option>
            <option>MARKET</option>
          </select>
        </label>
        <label>
          {t('order.label.price')}
          <input value={price} onChange={(event) => onStateChange({ side, type, price: event.currentTarget.value, qty })} />
        </label>
        <label>
          {t('order.label.qty')}
          <input value={qty} onChange={(event) => onStateChange({ side, type, price, qty: event.currentTarget.value })} />
        </label>
        <div className="order-actions">
          <button type="button" className="buy" disabled={!canSubmit}>
            {t('order.submitBuy')}
          </button>
          <button type="button" className="sell" disabled={!canSubmit}>
            {t('order.submitSell')}
          </button>
        </div>
        <p className="muted">{t('order.current', { side, type, price, qty })}</p>
      </form>
    </section>
  );
}
