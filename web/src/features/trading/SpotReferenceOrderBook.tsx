import { useLayoutEffect, useRef } from 'react';

import { formatMarketAtomic, formatMarketDecimal, marketAtomic } from './marketDecimal';

export type ReferenceLevel = { price: string; quantity: string };
export type ReferenceBook = { healthy: boolean; bids: ReferenceLevel[]; asks: ReferenceLevel[] };

/** 沿用合約訂單簿的面板與三欄結構；所有價量及累計均來自真實參考深度。 */
export function SpotReferenceOrderBook({ book, lastPrice, baseAsset, english, status, preview = false }: {
  book: ReferenceBook | null; lastPrice: string | null; baseAsset: string; english: boolean; status: string; preview?: boolean;
}) {
  const asksViewport = useRef<HTMLDivElement>(null);
  const followAsks = useRef(true);
  // 保持最佳賣價靠近中間報價；使用者捲動查看外側深度時，更新不能強制搶回位置。
  useLayoutEffect(() => {
    if (asksViewport.current && followAsks.current) asksViewport.current.scrollTop = asksViewport.current.scrollHeight;
  }, [book]);
  const bidTotal = book?.bids.reduce((sum, level) => sum + marketAtomic(level.quantity), 0n) ?? 0n;
  const askTotal = book?.asks.reduce((sum, level) => sum + marketAtomic(level.quantity), 0n) ?? 0n;
  // 百分比只用於畫面比例；先用整數計算至小數一位，再轉換 CSS 座標。
  const total = bidTotal + askTotal;
  const bidRatio = total > 0n ? Number(bidTotal * 1000n / total) / 10 : null;
  const askRatio = bidRatio === null ? null : 100 - bidRatio;

  function rows(levels: ReferenceLevel[], side: 'ask' | 'bid') {
    let cumulative = 0n;
    const values = levels.map(level => {
      cumulative += marketAtomic(level.quantity);
      return { ...level, cumulative };
    });
    // 賣盤的最佳價靠近中間報價；累計仍從最佳賣價向外計算，不隨畫面反轉。
    if (side === 'ask') values.reverse();
    return values.map(level => (
      <div className={`trading-book__row trading-book__row--${side}`} key={level.price}>
        <span className="trading-book__cell trading-book__cell--price">{formatMarketDecimal(level.price)}</span>
        <span className="trading-book__cell">{formatMarketDecimal(level.quantity)}</span>
        <span className="trading-book__cell">{formatMarketAtomic(level.cumulative)}</span>
      </div>
    ));
  }

  return (
    <section className="workspace-panel trading-book" aria-label={english ? 'Reference order book' : '參考訂單簿'}>
      <div className="workspace-panel__header trading-book__header">
        <h2>{english ? 'Order book' : '委託簿'}</h2><span>Binance · {preview ? (english ? 'quote preview' : '報價預覽') : (english ? 'Spot' : '現貨')}</span>
      </div>
      {book ? <div className="trading-book__sides">
        {(['asks', 'bids'] as const).map(side => (
          <div className="trading-book__side" key={side}>
            <p className={`trading-book__side-title trading-book__side-title--${side === 'asks' ? 'ask' : 'bid'}`}>{side === 'asks' ? (english ? 'Asks' : '賣單') : (english ? 'Bids' : '買單')}</p>
            <div className="trading-book__table-head"><span>{english ? 'Price' : '價格'} (USDT)</span><span>{preview ? (english ? 'Source quantity' : '來源數量') : (english ? 'Quantity' : '數量')} ({baseAsset})</span><span>{english ? 'Total' : '累計量'} ({baseAsset})</span></div>
            <div className="trading-book__rows" role="region" aria-label={side === 'asks' ? (english ? 'Ask depth, scroll for more' : '賣盤深度，可捲動查看') : (english ? 'Bid depth, scroll for more' : '買盤深度，可捲動查看')} tabIndex={0} ref={side === 'asks' ? asksViewport : undefined} onScroll={side === 'asks' ? event => { const node = event.currentTarget; followAsks.current = node.scrollHeight - node.clientHeight - node.scrollTop < 2; } : undefined}>{rows(book[side], side === 'asks' ? 'ask' : 'bid')}</div>
          </div>
        ))}
      </div> : <p className="balance-panel__empty" role="status">{status}</p>}
      <div className="trading-book__mid"><strong>{lastPrice ? formatMarketDecimal(lastPrice) : '—'}</strong><span>{english ? 'Reference price' : '參考價格'} · USDT</span></div>
      {preview && <p className="trading-page__adapter-note">{english ? 'Preview prices; source quantities are not reserved. Binance order routing is unavailable.' : '預覽價格；來源數量尚未保留，Binance 外部送單尚未啟用。'}</p>}
      <div className="trading-book__ratios">
        <div><p>{english ? 'Visible depth ratio · 20 levels per side' : '掛單比例 · 買賣各 20 檔'}</p>
          <div className="trading-book__ratio-labels"><span className="pnl--positive">{english ? 'Buy' : '買'} {bidRatio === null ? '—' : `${bidRatio.toFixed(1)}%`}</span><span className="pnl--negative">{english ? 'Sell' : '賣'} {askRatio === null ? '—' : `${askRatio.toFixed(1)}%`}</span></div>
          {bidRatio !== null && <div className="trading-book__ratio-bar" aria-hidden="true"><span className="trading-book__ratio-buy" style={{width: `${bidRatio}%`}} /><span className="trading-book__ratio-sell" style={{width: `${askRatio}%`}} /></div>}
        </div>
        <div><p>{english ? 'Taker ratio' : '吃單比例'}</p><p className="trading-page__adapter-note">{english ? 'Trade data is not available yet.' : '目前尚未提供成交資料。'}</p></div>
      </div>
    </section>
  );
}
