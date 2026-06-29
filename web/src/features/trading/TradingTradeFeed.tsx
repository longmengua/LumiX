import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { formatAmount, formatPrice, formatTime } from '../../utils/format';
import type { TradingFill } from './mockTradingService';

type TradingTradeFeedProps = {
  trades: TradingFill[];
};

export function TradingTradeFeed({ trades }: TradingTradeFeedProps) {
  const { t } = useI18n();

  return (
    <div className="trading-feed">
      {trades.map((trade, index) => (
        <article className="trading-feed__row" key={`${trade.time}-${index}`}>
          <div className="trading-feed__main">
            <Badge tone={trade.side === 'Buy' ? 'success' : 'danger'}>{t(trade.side === 'Buy' ? 'trading.trade.side.buy' : 'trading.trade.side.sell')}</Badge>
            <div>
              <p className="trading-feed__title">{formatPrice(trade.price, 2)}</p>
              <p className="trading-feed__meta">
                {formatAmount(trade.size)} {t('trading.trade.units')} · {t(trade.sourceKey)}
              </p>
            </div>
          </div>
          <span className="trading-feed__time">{formatTime(trade.time)}</span>
        </article>
      ))}
    </div>
  );
}
