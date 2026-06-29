import { Badge } from '../../components/base/Badge';
import { PriceText } from '../../components/trading/PriceText';
import { AmountText } from '../../components/trading/AmountText';
import { formatAmount, formatPrice } from '../../utils/format';
import type { TradingOrderBookLevel } from './mockTradingService';

type TradingOrderBookProps = {
  bids: TradingOrderBookLevel[];
  asks: TradingOrderBookLevel[];
  lastPrice: number;
};

export function TradingOrderBook({ bids, asks, lastPrice }: TradingOrderBookProps) {
  return (
    <div className="trading-book">
      <div className="trading-book__spread">
        <div>
          <p className="trading-book__label">Mid</p>
          <strong><PriceText value={lastPrice} /></strong>
        </div>
        <Badge tone="neutral">Development snapshot</Badge>
        <div className="trading-book__spread-value">
          <p className="trading-book__label">Synthetic spread</p>
          <strong>{formatPrice((asks[0]?.price ?? lastPrice) - (bids[0]?.price ?? lastPrice), 2)}</strong>
        </div>
      </div>

      <div className="trading-book__columns">
        <div>
          <p className="trading-book__side-label trading-book__side-label--ask">Asks</p>
          <div className="trading-book__rows">
            {asks.map((level, index) => (
              <div className="trading-book__row trading-book__row--ask" key={`${level.price}-${index}`}>
                <span className="trading-book__cell trading-book__cell--price">{formatPrice(level.price, 2)}</span>
                <span className="trading-book__cell"><AmountText value={level.size} /></span>
                <span className="trading-book__cell">{formatAmount(level.total, 2)}</span>
              </div>
            ))}
          </div>
        </div>

        <div>
          <p className="trading-book__side-label trading-book__side-label--bid">Bids</p>
          <div className="trading-book__rows">
            {bids.map((level, index) => (
              <div className="trading-book__row trading-book__row--bid" key={`${level.price}-${index}`}>
                <span className="trading-book__cell trading-book__cell--price">{formatPrice(level.price, 2)}</span>
                <span className="trading-book__cell"><AmountText value={level.size} /></span>
                <span className="trading-book__cell">{formatAmount(level.total, 2)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

