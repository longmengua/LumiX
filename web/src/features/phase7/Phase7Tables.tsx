import { Badge } from '../../components/base/Badge';
import { EmptyState } from '../../components/base/State';
import { AmountText } from '../../components/trading/AmountText';
import { PnlText } from '../../components/trading/PnlText';
import { PriceText } from '../../components/trading/PriceText';
import { formatAmount, formatCurrency, formatPercent, formatTime } from '../../utils/format';
import type {
  FundingRecord,
  FillRecord,
  LiquidationRecord,
  ManagedApiKey,
  NotificationRecord,
  OrderRecord,
  OrderStatus,
  PositionRecord,
} from './mockPhase7Service';

export function OrderCenterTable({ orders }: { orders: OrderRecord[] }) {
  if (orders.length === 0) {
    return <EmptyState title="No orders" description="The adapter snapshot does not contain any orders yet." />;
  }

  return (
    <div className="trading-table">
      <div className="trading-table__head trading-table__head--orders phase7-table__head--orders">
        <span>ID</span>
        <span>Symbol</span>
        <span>Venue</span>
        <span>Side</span>
        <span>Type</span>
        <span>Price</span>
        <span>Size</span>
        <span>Status</span>
      </div>

      {orders.map((order) => (
        <div className="trading-table__row trading-table__row--orders phase7-table__row--orders" key={order.id}>
          <span>{order.id}</span>
          <strong>{order.symbol}</strong>
          <span>{order.venue}</span>
          <Badge tone={order.side === 'Buy' ? 'success' : 'danger'}>{order.side}</Badge>
          <span>{order.type}</span>
          <span><PriceText value={order.price} /></span>
          <span><AmountText value={order.size} /></span>
          <Badge tone={getOrderTone(order.status)}>{order.status}</Badge>
        </div>
      ))}
    </div>
  );
}

export function OrderFillTable({ fills }: { fills: FillRecord[] }) {
  if (fills.length === 0) {
    return <EmptyState title="No fills" description="The adapter snapshot does not contain any fill records." />;
  }

  return (
    <div className="trading-table">
      <div className="trading-table__head phase7-table__head--fills">
        <span>Trade ID</span>
        <span>Order</span>
        <span>Symbol</span>
        <span>Side</span>
        <span>Price</span>
        <span>Size</span>
        <span>Fee</span>
        <span>Liquidity</span>
        <span>Time</span>
      </div>

      {fills.map((fill) => (
        <div className="trading-table__row phase7-table__row--fills" key={fill.id}>
          <span>{fill.id}</span>
          <span>{fill.orderId}</span>
          <strong>{fill.symbol}</strong>
          <Badge tone={fill.side === 'Buy' ? 'success' : 'danger'}>{fill.side}</Badge>
          <span><PriceText value={fill.price} /></span>
          <span><AmountText value={fill.size} /></span>
          <span>
            {formatAmount(fill.fee, 6)} {fill.feeAsset}
          </span>
          <Badge tone={fill.liquidity === 'Maker' ? 'success' : 'warning'}>{fill.liquidity}</Badge>
          <span>{formatTime(fill.createdAt)}</span>
        </div>
      ))}
    </div>
  );
}

export function PositionTable({ positions }: { positions: PositionRecord[] }) {
  if (positions.length === 0) {
    return <EmptyState title="No positions" description="The adapter snapshot does not contain any open positions." />;
  }

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
          <strong>{position.symbol}</strong>
          <Badge tone={position.side === 'Long' ? 'success' : 'danger'}>{position.side}</Badge>
          <span><AmountText value={position.size} /></span>
          <span><PriceText value={position.entryPrice} /></span>
          <span><PriceText value={position.markPrice} /></span>
          <span><PnlText value={position.pnl} /></span>
          <span>{position.leverage}x</span>
          <span>{formatCurrency(position.marginUsed)}</span>
          <span>
            <div className="trading-risk-cell">
              <div className="risk-bar">
                <span className="risk-bar__fill" style={{ width: `${Math.min(position.riskRatio, 100)}%` }} />
              </div>
              <strong>{formatPercent(position.riskRatio)}</strong>
            </div>
          </span>
        </div>
      ))}
    </div>
  );
}

export function LiquidationTable({ records }: { records: LiquidationRecord[] }) {
  if (records.length === 0) {
    return <EmptyState title="No liquidation records" description="The adapter snapshot does not contain liquidation history." />;
  }

  return (
    <div className="trading-table">
      <div className="trading-table__head phase7-table__head--liquidations">
        <span>ID</span>
        <span>Symbol</span>
        <span>Side</span>
        <span>Trigger</span>
        <span>Mark</span>
        <span>Status</span>
        <span>Reason</span>
        <span>Time</span>
      </div>

      {records.map((record) => (
        <div className="trading-table__row phase7-table__row--liquidations" key={record.id}>
          <span>{record.id}</span>
          <strong>{record.symbol}</strong>
          <Badge tone={record.side === 'Long' ? 'success' : 'danger'}>{record.side}</Badge>
          <span><PriceText value={record.triggerPrice} /></span>
          <span><PriceText value={record.markPrice} /></span>
          <Badge tone={getLiquidationTone(record.status)}>{record.status}</Badge>
          <span>{record.reason}</span>
          <span>{formatTime(record.createdAt)}</span>
        </div>
      ))}
    </div>
  );
}

export function FundingTable({ records }: { records: FundingRecord[] }) {
  if (records.length === 0) {
    return <EmptyState title="No funding records" description="The adapter snapshot does not contain funding history." />;
  }

  return (
    <div className="trading-table">
      <div className="trading-table__head phase7-table__head--funding">
        <span>ID</span>
        <span>Symbol</span>
        <span>Rate</span>
        <span>Payment</span>
        <span>Status</span>
        <span>Next funding</span>
        <span>Recorded</span>
      </div>

      {records.map((record) => (
        <div className="trading-table__row phase7-table__row--funding" key={record.id}>
          <span>{record.id}</span>
          <strong>{record.symbol}</strong>
          <span>{formatPercent(record.fundingRate, 3)}</span>
          <span>{formatCurrency(record.payment)}</span>
          <Badge tone={getFundingTone(record.status)}>{record.status}</Badge>
          <span>{formatTime(record.nextFundingAt)}</span>
          <span>{formatTime(record.createdAt)}</span>
        </div>
      ))}
    </div>
  );
}

export function NotificationList({
  notifications,
  onToggleRead,
}: {
  notifications: NotificationRecord[];
  onToggleRead: (id: string) => void;
}) {
  if (notifications.length === 0) {
    return <EmptyState title="No notifications" description="The adapter snapshot does not contain notification events." />;
  }

  return (
    <div className="timeline-list">
      {notifications.map((notification) => (
        <div className="timeline-item phase7-notification-row" key={notification.id}>
          <div className="phase7-notification-row__copy">
            <div className="notice-row">
              <Badge tone={getNotificationTone(notification.severity)}>{notification.category}</Badge>
              <Badge tone={notification.isRead ? 'neutral' : 'warning'}>{notification.isRead ? 'Read' : 'Unread'}</Badge>
            </div>
            <p className="account-row__title">{notification.title}</p>
            <p className="account-row__meta">{notification.description}</p>
          </div>
          <div className="phase7-notification-row__meta">
            <span>{formatTime(notification.createdAt)}</span>
            <button className="secondary-button" type="button" onClick={() => onToggleRead(notification.id)}>
              {notification.isRead ? 'Mark unread' : 'Mark read'}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

export function ApiKeyList({ keys }: { keys: ManagedApiKey[] }) {
  if (keys.length === 0) {
    return <EmptyState title="No API keys" description="Create a key to use the development adapter." />;
  }

  return (
    <div className="api-key-list">
      {keys.map((key) => (
        <div className="api-key-card" key={key.name}>
          <div className="api-key-card__header">
            <div>
              <p className="account-row__title">{key.name}</p>
              <p className="account-row__meta">Secret is shown once on creation and is masked here.</p>
            </div>
            <Badge tone={key.status === 'Active' ? 'success' : 'warning'}>{key.status}</Badge>
          </div>

          <div className="api-key-card__body">
            <div className="stat-list">
              <div className="stat-list__row">
                <dt>Secret</dt>
                <dd>{key.secretMasked}</dd>
              </div>
              <div className="stat-list__row">
                <dt>Permissions</dt>
                <dd>{key.permissions.join(', ')}</dd>
              </div>
              <div className="stat-list__row">
                <dt>IP whitelist</dt>
                <dd>{key.ipWhitelist}</dd>
              </div>
              <div className="stat-list__row">
                <dt>Last used</dt>
                <dd>{formatTime(key.lastUsedAt)}</dd>
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function getOrderTone(status: OrderStatus) {
  switch (status) {
    case 'Filled':
      return 'success';
    case 'Partially Filled':
      return 'warning';
    case 'Canceled':
      return 'neutral';
    case 'Rejected':
      return 'danger';
    default:
      return 'neutral';
  }
}

function getLiquidationTone(status: LiquidationRecord['status']) {
  switch (status) {
    case 'Triggered':
      return 'danger';
    case 'Prevented':
      return 'success';
    default:
      return 'warning';
  }
}

function getFundingTone(status: FundingRecord['status']) {
  switch (status) {
    case 'Paid':
      return 'success';
    case 'Due':
      return 'warning';
    default:
      return 'neutral';
  }
}

function getNotificationTone(severity: NotificationRecord['severity']) {
  switch (severity) {
    case 'success':
      return 'success';
    case 'warning':
      return 'warning';
    default:
      return 'neutral';
  }
}
