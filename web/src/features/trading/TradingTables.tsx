import { Badge } from '../../components/base/Badge';
import { RiskRatioBar } from '../../components/trading/RiskRatioBar';
import { PnlText } from '../../components/trading/PnlText';
import { PriceText } from '../../components/trading/PriceText';
import { AmountText } from '../../components/trading/AmountText';
import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPercent } from '../../utils/format';
import type { TradingOpenOrder, TradingPosition } from './mockTradingService';

const orderSideKeys = {
  Buy: 'trading.order.side.buy',
  Sell: 'trading.order.side.sell',
} as const;

const orderTypeKeys = {
  Limit: 'trading.order.type.limit',
  Market: 'trading.order.type.market',
  Stop: 'trading.order.type.stop',
} as const;

const orderStatusKeys = {
  Working: 'trading.order.status.working',
  'Partially Filled': 'trading.order.status.partiallyFilled',
  Queued: 'trading.order.status.queued',
} as const;

const positionSideKeys = {
  Long: 'trading.position.side.long',
  Short: 'trading.position.side.short',
} as const;

type TradingOpenOrdersTableProps = {
  orders: TradingOpenOrder[];
};

export function TradingOpenOrdersTable({ orders }: TradingOpenOrdersTableProps) {
  const { t } = useI18n();

  return (
    <div className="trading-table">
      <div className="trading-table__head trading-table__head--orders">
        <span>{t('trading.table.id')}</span>
        <span>{t('trading.order.side')}</span>
        <span>{t('trading.order.type')}</span>
        <span>{t('trading.table.price')}</span>
        <span>{t('trading.table.size')}</span>
        <span>{t('trading.table.filled')}</span>
        <span>{t('trading.table.status')}</span>
      </div>

      {orders.map((order) => (
        <div className="trading-table__row trading-table__row--orders" key={order.id}>
          <span>{order.id}</span>
          <Badge tone={order.side === 'Buy' ? 'success' : 'danger'}>{t(orderSideKeys[order.side])}</Badge>
          <span>{t(orderTypeKeys[order.type])}</span>
          <span><PriceText value={order.price} /></span>
          <span><AmountText value={order.size} /></span>
          <span>{formatAmount(order.filled)}</span>
          <Badge tone={order.status === 'Working' ? 'neutral' : order.status === 'Partially Filled' ? 'warning' : 'success'}>
            {t(orderStatusKeys[order.status])}
          </Badge>
        </div>
      ))}
    </div>
  );
}

type TradingPositionsTableProps = {
  positions: TradingPosition[];
};

export function TradingPositionsTable({ positions }: TradingPositionsTableProps) {
  const { t } = useI18n();

  return (
    <div className="trading-table">
      <div className="trading-table__head trading-table__head--positions">
        <span>{t('trading.instrument.symbol')}</span>
        <span>{t('trading.position.side')}</span>
        <span>{t('trading.table.size')}</span>
        <span>{t('trading.table.entry')}</span>
        <span>{t('trading.table.mark')}</span>
        <span>{t('trading.table.pnl')}</span>
        <span>{t('trading.table.leverage')}</span>
        <span>{t('trading.table.margin')}</span>
        <span>{t('trading.risk.title')}</span>
      </div>

      {positions.map((position) => (
        <div className="trading-table__row trading-table__row--positions" key={`${position.symbol}-${position.side}`}>
          <span>{position.symbol}</span>
          <Badge tone={position.side === 'Long' ? 'success' : 'danger'}>{t(positionSideKeys[position.side])}</Badge>
          <span><AmountText value={position.size} /></span>
          <span><PriceText value={position.entryPrice} /></span>
          <span><PriceText value={position.markPrice} /></span>
          <span><PnlText value={position.pnl} /></span>
          <span>{position.leverage}x</span>
          <span>{formatCurrency(position.marginUsed)}</span>
          <span>
            <div className="trading-risk-cell">
              <RiskRatioBar ratio={position.riskRatio} />
              <strong>{formatPercent(position.riskRatio)}</strong>
            </div>
          </span>
        </div>
      ))}
    </div>
  );
}

type TradingRiskSnapshotProps = {
  riskRatio: number;
  fundingOrBorrow: string;
};

export function TradingRiskSnapshot({ riskRatio, fundingOrBorrow }: TradingRiskSnapshotProps) {
  const { t } = useI18n();

  return (
    <div className="trading-risk">
      <div className="trading-risk__metrics">
        <div className="stat-card">
          <span className="stat-card__label">{t('trading.risk.riskRatio')}</span>
          <strong>{formatPercent(riskRatio)}</strong>
        </div>
        <div className="stat-card">
          <span className="stat-card__label">{t('trading.risk.fundingOrBorrow')}</span>
          <strong>{fundingOrBorrow}</strong>
        </div>
      </div>
      <RiskRatioBar ratio={riskRatio} />
      <p className="trading-risk__hint">
        {t('trading.risk.snapshotHint')}
      </p>
    </div>
  );
}
