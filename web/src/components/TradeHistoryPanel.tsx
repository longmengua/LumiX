import { useRef } from 'react';
import { TradeRow } from '../types/exchange';
import { useI18n } from '../lib/i18n';

/**
 * Read-only execution history list, updated from REST bootstrap + websocket stream.
 * Scroll container is fixed height and requests next page when reaching bottom.
 */
interface Props {
  trades: readonly TradeRow[];
  loading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
}

export function TradeHistoryPanel({ trades, loading, hasMore, onLoadMore }: Props) {
  const { t } = useI18n();
  const listRef = useRef<HTMLDivElement>(null);
  const LOAD_MORE_OFFSET = 10;

  const handleScroll = () => {
    const list = listRef.current;
    if (!list || loading || !hasMore) {
      return;
    }

    const atBottom = list.scrollTop + list.clientHeight >= list.scrollHeight - LOAD_MORE_OFFSET;
    if (atBottom) {
      onLoadMore();
    }
  };

  return (
      <section className="panel trade-history-panel">
        <div className="panel-head">
        <h2>{t('history.title')}</h2>
      </div>
        <div className="table-scroll" ref={listRef} onScroll={handleScroll}>
          <div className="table-head">
          <span>{t('history.heading.time')}</span>
          <span>{t('history.heading.side')}</span>
          <span>{t('history.heading.price')}</span>
          <span>{t('history.heading.qty')}</span>
          <span>{t('history.heading.notional')}</span>
        </div>
        {trades.length === 0 ? (
          <div className="muted no-data">{t('history.empty')}</div>
        ) : (
          trades.map((trade) => (
            <div className="table-row" key={`${trade.matchId}-${trade.tsKey}`}>
              <span>{trade.time}</span>
              <span className={trade.side === 'BUY' ? 'up' : 'down'}>{trade.side}</span>
              <span>{trade.price}</span>
              <span>{trade.qty}</span>
              <span>{trade.notional}</span>
            </div>
          ))
        )}
        {loading && <div className="muted trade-loading">{t('history.loading')}</div>}
        {!loading && !hasMore && trades.length > 0 && <div className="muted trade-end">{t('history.end')}</div>}
      </div>
    </section>
  );
}
