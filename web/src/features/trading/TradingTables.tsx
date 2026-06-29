import { Badge } from '../../components/base/Badge';
import { RiskRatioBar } from '../../components/trading/RiskRatioBar';
import { PnlText } from '../../components/trading/PnlText';
import { PriceText } from '../../components/trading/PriceText';
import { AmountText } from '../../components/trading/AmountText';
import { formatAmount, formatCurrency, formatPercent } from '../../utils/format';
import type { TradingOpenOrder, TradingPosition } from './mockTradingService';

type TradingOpenOrdersTableProps = {
  orders: TradingOpenOrder[];
};

export function TradingOpenOrdersTable({ orders }: TradingOpenOrdersTableProps) {
  return (
    <div className="trading-table">
      <div className="trading-table__head trading-table__head--orders">
        <span>ID</span>
        <span>Side</span>
        <span>Type</span>
        <span>Price</span>
        <span>Size</span>
        <span>Filled</span>
        <span>Status</span>
      </div>

      {orders.map((order) => (
        <div className="trading-table__row trading-table__row--orders" key={order.id}>
          <span>{order.id}</span>
          <Badge tone={order.side === 'Buy' ? 'success' : 'danger'}>{order.side}</Badge>
          <span>{order.type}</span>
          <span><PriceText value={order.price} /></span>
          <span><AmountText value={order.size} /></span>
          <span>{formatAmount(order.filled)}</span>
          <Badge tone={order.status === 'Working' ? 'neutral' : order.status === 'Partially Filled' ? 'warning' : 'success'}>
            {order.status}
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
  return (
    <div className="trading-table">
      <div className="trading-table__head trading-table__head--positions">
        <span>Symbol</span>
        <span>Side</span>
        <span>Size</span>
        <span>Entry</span>
        <span>Mark</span>
        <span>PnL</span>
        <span>Leverage</span>
        <span>Margin</span>
        <span>Risk</span>
      </div>

      {positions.map((position) => (
        <div className="trading-table__row trading-table__row--positions" key={`${position.symbol}-${position.side}`}>
          <span>{position.symbol}</span>
          <Badge tone={position.side === 'Long' ? 'success' : 'danger'}>{position.side}</Badge>
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
  return (
    <div className="trading-risk">
      <div className="trading-risk__metrics">
        <div className="stat-card">
          <span className="stat-card__label">Risk ratio</span>
          <strong>{formatPercent(riskRatio)}</strong>
        </div>
        <div className="stat-card">
          <span className="stat-card__label">Funding / borrow</span>
          <strong>{fundingOrBorrow}</strong>
        </div>
      </div>
      <RiskRatioBar ratio={riskRatio} />
      <p className="trading-risk__hint">
        This is an adapter-only snapshot. It does not calculate live margin risk, liquidation, or borrow events.
      </p>
    </div>
  );
}
