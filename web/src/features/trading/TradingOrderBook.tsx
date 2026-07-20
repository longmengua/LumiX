import { useState } from 'react';

import { useI18n } from '../../i18n';
import { formatAmount, formatPercent, formatPrice } from '../../utils/format';
import type { TradingFill, TradingOrderBookLevel } from './mockTradingService';

type TradingOrderBookProps = {
  bids: TradingOrderBookLevel[];
  asks: TradingOrderBookLevel[];
  trades: TradingFill[];
  lastPrice: number;
  markPrice: number;
  baseAsset: string;
  onSelectPrice: (price: number) => void;
};

type TradingRatios = {
  bid: number;
  ask: number;
  takerBuy: number;
  takerSell: number;
};

export function calculateOrderBookRatios(bids: TradingOrderBookLevel[], asks: TradingOrderBookLevel[], trades: TradingFill[]): TradingRatios {
  // 掛單比例與吃單比例必須各自取數：前者讀可見深度，後者只讀成交主動方。
  const bidSize = bids.reduce((sum, level) => sum + level.size, 0);
  const askSize = asks.reduce((sum, level) => sum + level.size, 0);
  const buyTakerSize = trades.filter((trade) => trade.aggressorSide === 'Buy').reduce((sum, trade) => sum + trade.size, 0);
  const sellTakerSize = trades.filter((trade) => trade.aggressorSide === 'Sell').reduce((sum, trade) => sum + trade.size, 0);
  const bookTotal = bidSize + askSize;
  const takerTotal = buyTakerSize + sellTakerSize;

  return {
    bid: bookTotal === 0 ? 0 : bidSize / bookTotal,
    ask: bookTotal === 0 ? 0 : askSize / bookTotal,
    takerBuy: takerTotal === 0 ? 0 : buyTakerSize / takerTotal,
    takerSell: takerTotal === 0 ? 0 : sellTakerSize / takerTotal,
  };
}

function RatioBar({ buyRatio, sellRatio, buyLabel, sellLabel }: { buyRatio: number; sellRatio: number; buyLabel: string; sellLabel: string }) {
  return (
    <div className="trading-book__ratio">
      <div className="trading-book__ratio-labels">
        <span className="pnl--positive">{buyLabel} {formatPercent(buyRatio * 100, 1)}</span>
        <span className="pnl--negative">{sellLabel} {formatPercent(sellRatio * 100, 1)}</span>
      </div>
      <div className="trading-book__ratio-bar" aria-hidden="true">
        <span className="trading-book__ratio-buy" style={{ width: `${buyRatio * 100}%` }} />
        <span className="trading-book__ratio-sell" style={{ width: `${sellRatio * 100}%` }} />
      </div>
    </div>
  );
}

export function TradingOrderBook({ bids, asks, trades, lastPrice, markPrice, baseAsset, onSelectPrice }: TradingOrderBookProps) {
  const { t } = useI18n();
  const [tickSize, setTickSize] = useState('0.01');
  const ratios = calculateOrderBookRatios(bids, asks, trades);
  const fractionDigits = tickSize === '1' ? 0 : tickSize === '0.1' ? 1 : 2;

  function renderRows(levels: TradingOrderBookLevel[], side: 'ask' | 'bid') {
    const orderedLevels = side === 'ask' ? [...levels].reverse() : levels;
    return orderedLevels.map((level) => (
      <button
        className={`trading-book__row trading-book__row--${side}`}
        key={`${side}-${level.price}`}
        type="button"
        onClick={() => onSelectPrice(level.price)}
      >
        <span className="trading-book__cell trading-book__cell--price">{formatPrice(level.price, fractionDigits)}</span>
        <span className="trading-book__cell">{formatAmount(level.size)}</span>
        <span className="trading-book__cell">{formatAmount(level.total, 2)}</span>
      </button>
    ));
  }

  return (
    <section className="workspace-panel trading-book">
      <div className="workspace-panel__header trading-book__header">
        <h2>{t('trading.orderBook.title')}</h2>
        <label>
          <span>{t('trading.orderBook.tickSize')}</span>
          <select value={tickSize} onChange={(event) => setTickSize(event.target.value)}>
            <option value="0.01">0.01</option>
            <option value="0.1">0.1</option>
            <option value="1">1</option>
          </select>
        </label>
      </div>

      <div className="trading-book__sides">
        <div className="trading-book__side">
          <p className="trading-book__side-title trading-book__side-title--ask">{t('trading.orderBook.asks')}</p>
          <div className="trading-book__table-head">
            <span>{t('trading.orderBook.priceColumn', undefined, { currency: 'USDT' })}</span>
            <span>{t('trading.orderBook.quantityColumn', undefined, { asset: baseAsset })}</span>
            <span>{t('trading.orderBook.totalColumn', undefined, { asset: baseAsset })}</span>
          </div>
          <div className="trading-book__rows">{renderRows(asks, 'ask')}</div>
        </div>
        <div className="trading-book__side">
          <p className="trading-book__side-title trading-book__side-title--bid">{t('trading.orderBook.bids')}</p>
          <div className="trading-book__table-head">
            <span>{t('trading.orderBook.priceColumn', undefined, { currency: 'USDT' })}</span>
            <span>{t('trading.orderBook.quantityColumn', undefined, { asset: baseAsset })}</span>
            <span>{t('trading.orderBook.totalColumn', undefined, { asset: baseAsset })}</span>
          </div>
          <div className="trading-book__rows">{renderRows(bids, 'bid')}</div>
        </div>
      </div>

      <button className="trading-book__mid" type="button" onClick={() => onSelectPrice(lastPrice)}>
        <strong>{formatPrice(lastPrice, fractionDigits)}</strong>
        <span>{t('trading.orderBook.markPrice')} {formatPrice(markPrice, fractionDigits)}</span>
      </button>
      <div className="trading-book__ratios">
        <div>
          <p>{t('trading.orderBook.restingRatio')}</p>
          <RatioBar buyRatio={ratios.bid} sellRatio={ratios.ask} buyLabel={t('trading.orderBook.bid')} sellLabel={t('trading.orderBook.ask')} />
        </div>
        <div>
          <p>{t('trading.orderBook.takerRatio')}</p>
          <RatioBar buyRatio={ratios.takerBuy} sellRatio={ratios.takerSell} buyLabel={t('trading.orderBook.takerBuy')} sellLabel={t('trading.orderBook.takerSell')} />
        </div>
      </div>
    </section>
  );
}
