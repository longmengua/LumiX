import { useEffect, useState, type FormEvent } from 'react';

import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPrice } from '../../utils/format';
import type { TradingKind, TradingWorkspaceData } from './mockTradingService';

type TradingOrderFormProps = {
  kind: TradingKind;
  workspace: TradingWorkspaceData;
  selectedBookPrice: number | null;
};

type OrderSide = 'Buy' | 'Sell';
type OrderType = 'Limit' | 'Market';

export function TradingOrderForm({ kind, workspace, selectedBookPrice }: TradingOrderFormProps) {
  const { t } = useI18n();
  const [side, setSide] = useState<OrderSide>('Buy');
  const [orderType, setOrderType] = useState<OrderType>('Limit');
  const [price, setPrice] = useState(String(workspace.orderBook.bids[0]?.price ?? 0));
  const [quantity, setQuantity] = useState('0.25');
  const [leverage, setLeverage] = useState(kind === 'futures' ? '5' : '1');
  const [sizePercent, setSizePercent] = useState(0);
  const [messageKey, setMessageKey] = useState<string | null>(null);

  useEffect(() => {
    // 切換市場才重置輸入；點委託簿時要保留使用者已選擇的方向與數量。
    setPrice(String(workspace.orderBook.bids[0]?.price ?? 0));
    setQuantity('0.25');
    setLeverage(kind === 'futures' ? '5' : '1');
    setSizePercent(0);
    setMessageKey(null);
  }, [kind, workspace]);

  useEffect(() => {
    if (selectedBookPrice === null) {
      return;
    }

    // 點價預填必須轉成限價單，且只改表單 state，完全不觸發 preview 或送單。
    setOrderType('Limit');
    setPrice(selectedBookPrice.toFixed(2));
  }, [selectedBookPrice]);

  const priceValue = Number(price) || workspace.orderBook.bids[0]?.price || 0;
  const quantityValue = Number(quantity) || 0;
  const notional = priceValue * quantityValue;
  const availableBalance = workspace.balances[0]?.available ?? 0;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // 這裡只回報 local saved 狀態；真正下單必須經過 API、風控、撮合與結算。
    setMessageKey('trading.order.savedLocally');
  }

  function handleSizePercent(percent: number) {
    setSizePercent(percent);
    if (priceValue > 0) {
      setQuantity(((availableBalance * (percent / 100)) / priceValue).toFixed(4));
    }
  }

  return (
    <form className="trading-form workspace-panel" onSubmit={handleSubmit}>
      <div className="trading-form__toolbar">
        <button className={`tab-button${side === 'Buy' ? ' tab-button--active' : ''}`} type="button" onClick={() => setSide('Buy')}>
          {t('trading.order.side.buy')}
        </button>
        <button className={`tab-button${side === 'Sell' ? ' tab-button--active' : ''}`} type="button" onClick={() => setSide('Sell')}>
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
              {['2', '3', '5', '8', '10'].map((item) => <option key={item} value={item}>{item}x</option>)}
            </select>
          </label>
        ) : null}
        <label className="field">
          <span className="field__label">{t('trading.order.price')} (USDT)</span>
          <input className="input" inputMode="decimal" value={price} onChange={(event) => setPrice(event.target.value)} />
        </label>
        <label className="field">
          <span className="field__label">{t('trading.order.quantity')} ({workspace.baseAsset})</span>
          <input className="input" inputMode="decimal" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
        </label>
      </div>

      <div className="trading-form__allocation">
        <input aria-label={t('trading.order.sizePercent')} type="range" min="0" max="100" step="25" value={sizePercent} onChange={(event) => handleSizePercent(Number(event.target.value))} />
        <div>{[0, 25, 50, 75, 100].map((percent) => <button className={sizePercent === percent ? 'trading-form__allocation-button trading-form__allocation-button--active' : 'trading-form__allocation-button'} key={percent} type="button" onClick={() => handleSizePercent(percent)}>{percent}%</button>)}</div>
      </div>

      <div className="trading-form__summary">
        <div className="trading-form__summary-row"><span>{t('trading.order.previewNotional')}</span><strong>{formatCurrency(notional)}</strong></div>
        <div className="trading-form__summary-row"><span>{t('trading.order.approxSize')}</span><strong>{formatAmount(quantityValue)} {workspace.baseAsset}</strong></div>
        <div className="trading-form__summary-row"><span>{t('trading.order.previewPrice')}</span><strong>{formatPrice(priceValue, 2)}</strong></div>
      </div>

      <p className="trading-form__hint">{t('trading.order.availableMockBalance')} {formatCurrency(availableBalance)}</p>
      {messageKey ? <p className="form-message form-message--success">{t(messageKey)}</p> : null}
      <button className={`primary-button trading-form__submit${side === 'Sell' ? ' trading-form__submit--sell' : ''}`} type="submit">{t(workspace.actionLabelKey)}</button>
    </form>
  );
}
