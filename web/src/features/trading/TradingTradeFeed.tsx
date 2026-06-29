import { Badge } from '../../components/base/Badge';
import { formatAmount, formatPrice, formatTime } from '../../utils/format';
import type { TradingFill } from './mockTradingService';

type TradingTradeFeedProps = {
  trades: TradingFill[];
};

export function TradingTradeFeed({ trades }: TradingTradeFeedProps) {
  return (
    <div className="trading-feed">
      {trades.map((trade, index) => (
        <article className="trading-feed__row" key={`${trade.time}-${index}`}>
          <div className="trading-feed__main">
            <Badge tone={trade.side === 'Buy' ? 'success' : 'danger'}>{trade.side}</Badge>
            <div>
              <p className="trading-feed__title">{formatPrice(trade.price, 2)}</p>
              <p className="trading-feed__meta">
                {formatAmount(trade.size)} units · {trade.source}
              </p>
            </div>
          </div>
          <span className="trading-feed__time">{formatTime(trade.time)}</span>
        </article>
      ))}
    </div>
  );
}

