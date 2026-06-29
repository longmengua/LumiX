const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type OrderStatus = 'Open' | 'Partially Filled' | 'Filled' | 'Canceled' | 'Rejected';

export type OrderRecord = {
  id: string;
  symbol: string;
  venue: 'Spot' | 'Futures' | 'Margin';
  side: 'Buy' | 'Sell';
  type: 'Limit' | 'Market' | 'Stop' | 'TWAP';
  status: OrderStatus;
  price: number;
  size: number;
  filled: number;
  avgFillPrice: number;
  timeInForce: 'GTC' | 'IOC' | 'FOK';
  createdAt: string;
  updatedAt: string;
};

export type FillRecord = {
  id: string;
  orderId: string;
  symbol: string;
  side: 'Buy' | 'Sell';
  price: number;
  size: number;
  fee: number;
  feeAsset: string;
  liquidity: 'Maker' | 'Taker';
  createdAt: string;
};

export type OrderCenterSnapshot = {
  adapterNotice: string;
  summary: Array<{ label: string; value: string; hint: string }>;
  openOrders: OrderRecord[];
  orderHistory: OrderRecord[];
  fills: FillRecord[];
};

export type PositionRecord = {
  symbol: string;
  side: 'Long' | 'Short';
  size: number;
  entryPrice: number;
  markPrice: number;
  pnl: number;
  leverage: number;
  marginUsed: number;
  liqPrice: number;
  riskRatio: number;
};

export type LiquidationRecord = {
  id: string;
  symbol: string;
  side: 'Long' | 'Short';
  triggerPrice: number;
  markPrice: number;
  status: 'Monitoring' | 'Triggered' | 'Prevented';
  reason: string;
  createdAt: string;
};

export type FundingRecord = {
  id: string;
  symbol: string;
  fundingRate: number;
  payment: number;
  status: 'Paid' | 'Due' | 'Projected';
  nextFundingAt: string;
  createdAt: string;
};

export type PositionCenterSnapshot = {
  adapterNotice: string;
  summary: Array<{ label: string; value: string; hint: string }>;
  positions: PositionRecord[];
  liquidationRecords: LiquidationRecord[];
  fundingRecords: FundingRecord[];
};

export type ApiPermission = 'read' | 'spot trade' | 'futures trade' | 'margin trade' | 'withdraw';

export type ManagedApiKey = {
  name: string;
  status: 'Active' | 'Paused';
  permissions: ApiPermission[];
  ipWhitelist: string;
  lastUsedAt: string;
  createdAt: string;
  secretMasked: string;
};

export type ApiKeySnapshot = {
  adapterNotice: string;
  keys: ManagedApiKey[];
  permissionDefaults: ApiPermission[];
  secretPolicy: string;
};

export type NotificationRecord = {
  id: string;
  title: string;
  description: string;
  category: 'Security' | 'Orders' | 'Positions' | 'Funding' | 'System';
  severity: 'info' | 'success' | 'warning';
  isRead: boolean;
  createdAt: string;
};

export type NotificationSnapshot = {
  adapterNotice: string;
  summary: Array<{ label: string; value: string; hint: string }>;
  notifications: NotificationRecord[];
};

const orderCenterData: OrderCenterSnapshot = {
  adapterNotice:
    'Development adapter only. OL before must connect server/ Java order API, C++ Core order events, and settlement state from the matching stack.',
  summary: [
    { label: 'Open orders', value: '3', hint: 'Local snapshot only, no live order routing.' },
    { label: 'Completed', value: '18', hint: 'History is illustrative and not exchange-backed.' },
    { label: 'Fill ratio', value: '91.4%', hint: 'Derived from adapter data, not production fills.' },
    { label: 'Pending cancels', value: '1', hint: 'No cancel or replace is sent to a live venue.' },
  ],
  openOrders: [
    {
      id: 'ord-2048',
      symbol: 'BTC-USDT',
      venue: 'Spot',
      side: 'Buy',
      type: 'Limit',
      status: 'Partially Filled',
      price: 68432.1,
      size: 0.48,
      filled: 0.14,
      avgFillPrice: 68428.7,
      timeInForce: 'GTC',
      createdAt: '2026-06-29T01:32:00Z',
      updatedAt: '2026-06-29T01:41:00Z',
    },
    {
      id: 'ord-2049',
      symbol: 'ETH-USDT',
      venue: 'Margin',
      side: 'Sell',
      type: 'Stop',
      status: 'Open',
      price: 3692.2,
      size: 6.2,
      filled: 0,
      avgFillPrice: 0,
      timeInForce: 'GTC',
      createdAt: '2026-06-29T02:12:00Z',
      updatedAt: '2026-06-29T02:12:00Z',
    },
    {
      id: 'ord-2050',
      symbol: 'SOL-USDT',
      venue: 'Futures',
      side: 'Buy',
      type: 'Market',
      status: 'Open',
      price: 167.45,
      size: 140,
      filled: 32,
      avgFillPrice: 167.39,
      timeInForce: 'IOC',
      createdAt: '2026-06-29T02:33:00Z',
      updatedAt: '2026-06-29T02:33:00Z',
    },
  ],
  orderHistory: [
    {
      id: 'ord-2031',
      symbol: 'BTC-USDT',
      venue: 'Spot',
      side: 'Sell',
      type: 'Limit',
      status: 'Filled',
      price: 68310.5,
      size: 0.21,
      filled: 0.21,
      avgFillPrice: 68316.4,
      timeInForce: 'GTC',
      createdAt: '2026-06-28T18:02:00Z',
      updatedAt: '2026-06-28T18:02:12Z',
    },
    {
      id: 'ord-2032',
      symbol: 'ETH-USDT',
      venue: 'Futures',
      side: 'Buy',
      type: 'TWAP',
      status: 'Canceled',
      price: 3632.1,
      size: 24,
      filled: 12,
      avgFillPrice: 3635.8,
      timeInForce: 'IOC',
      createdAt: '2026-06-28T16:08:00Z',
      updatedAt: '2026-06-28T16:14:00Z',
    },
    {
      id: 'ord-2033',
      symbol: 'SOL-USDT',
      venue: 'Margin',
      side: 'Buy',
      type: 'Limit',
      status: 'Rejected',
      price: 164.8,
      size: 80,
      filled: 0,
      avgFillPrice: 0,
      timeInForce: 'FOK',
      createdAt: '2026-06-28T12:21:00Z',
      updatedAt: '2026-06-28T12:21:08Z',
    },
  ],
  fills: [
    {
      id: 'fill-8791',
      orderId: 'ord-2031',
      symbol: 'BTC-USDT',
      side: 'Sell',
      price: 68316.4,
      size: 0.21,
      fee: 0.00042,
      feeAsset: 'BTC',
      liquidity: 'Maker',
      createdAt: '2026-06-28T18:02:12Z',
    },
    {
      id: 'fill-8792',
      orderId: 'ord-2032',
      symbol: 'ETH-USDT',
      side: 'Buy',
      price: 3635.8,
      size: 12,
      fee: 4.35,
      feeAsset: 'USDT',
      liquidity: 'Taker',
      createdAt: '2026-06-28T16:09:15Z',
    },
    {
      id: 'fill-8793',
      orderId: 'ord-2048',
      symbol: 'BTC-USDT',
      side: 'Buy',
      price: 68428.7,
      size: 0.14,
      fee: 0.00028,
      feeAsset: 'BTC',
      liquidity: 'Maker',
      createdAt: '2026-06-29T01:41:00Z',
    },
  ],
};

const positionCenterData: PositionCenterSnapshot = {
  adapterNotice:
    'Development adapter only. OL before must connect server/ Java position API, C++ Core settlement events, and the funding / liquidation pipeline.',
  summary: [
    { label: 'Open positions', value: '2', hint: 'Snapshot only, not a live risk engine.' },
    { label: 'Liquidation watch', value: '1', hint: 'Monitoring records are informational only.' },
    { label: 'Next funding', value: '08:00 UTC', hint: 'Projected by adapter data, not exchange feed.' },
    { label: 'Net PnL', value: '+$4,218.60', hint: 'Local estimate only.' },
  ],
  positions: [
    {
      symbol: 'BTCUSDT-PERP',
      side: 'Long',
      size: 3.5,
      entryPrice: 67912.8,
      markPrice: 68450.3,
      pnl: 1881.25,
      leverage: 5,
      marginUsed: 23980.2,
      liqPrice: 59492.4,
      riskRatio: 24.8,
    },
    {
      symbol: 'ETHUSDT-PERP',
      side: 'Short',
      size: 18,
      entryPrice: 3678.4,
      markPrice: 3621.2,
      pnl: 1027.35,
      leverage: 4,
      marginUsed: 16423.1,
      liqPrice: 4118.6,
      riskRatio: 31.2,
    },
  ],
  liquidationRecords: [
    {
      id: 'liq-4511',
      symbol: 'BTCUSDT-PERP',
      side: 'Long',
      triggerPrice: 59602.0,
      markPrice: 59581.2,
      status: 'Monitoring',
      reason: 'Maintenance margin threshold being watched in the adapter snapshot.',
      createdAt: '2026-06-29T01:05:00Z',
    },
    {
      id: 'liq-4512',
      symbol: 'SOLUSDT-PERP',
      side: 'Short',
      triggerPrice: 181.2,
      markPrice: 178.4,
      status: 'Prevented',
      reason: 'Synthetic risk check kept the example position above the threshold.',
      createdAt: '2026-06-28T20:40:00Z',
    },
  ],
  fundingRecords: [
    {
      id: 'fund-2201',
      symbol: 'BTCUSDT-PERP',
      fundingRate: 0.012,
      payment: 12.48,
      status: 'Paid',
      nextFundingAt: '2026-06-29T08:00:00Z',
      createdAt: '2026-06-29T00:00:00Z',
    },
    {
      id: 'fund-2202',
      symbol: 'ETHUSDT-PERP',
      fundingRate: -0.004,
      payment: -6.32,
      status: 'Due',
      nextFundingAt: '2026-06-29T08:00:00Z',
      createdAt: '2026-06-29T04:00:00Z',
    },
    {
      id: 'fund-2203',
      symbol: 'SOLUSDT-PERP',
      fundingRate: 0.006,
      payment: 0,
      status: 'Projected',
      nextFundingAt: '2026-06-29T16:00:00Z',
      createdAt: '2026-06-29T08:00:00Z',
    },
  ],
};

const apiKeySnapshot: ApiKeySnapshot = {
  adapterNotice:
    'Development adapter only. OL before must connect server/ Java API key service and security audit trail.',
  keys: [
    {
      name: 'MM-BOT-01',
      status: 'Active',
      permissions: ['read', 'spot trade', 'futures trade'],
      ipWhitelist: '203.0.113.10, 203.0.113.11',
      lastUsedAt: '2026-06-29T02:21:00Z',
      createdAt: '2026-06-20T08:02:00Z',
      secretMasked: 'LXAK...8C2F',
    },
    {
      name: 'Admin-ReadOnly',
      status: 'Paused',
      permissions: ['read'],
      ipWhitelist: '192.0.2.34',
      lastUsedAt: '2026-06-24T11:55:00Z',
      createdAt: '2026-06-18T03:22:00Z',
      secretMasked: 'LXAK...19AA',
    },
  ],
  permissionDefaults: ['read', 'spot trade', 'futures trade', 'margin trade'],
  secretPolicy: 'Secrets are displayed once at creation only and are not persisted in this development adapter.',
};

const notificationSnapshot: NotificationSnapshot = {
  adapterNotice:
    'Development adapter only. OL before must connect server/ Java notification service and the event pipeline.',
  summary: [
    { label: 'Unread', value: '3', hint: 'Local state only; no push service is wired.' },
    { label: 'Security', value: '2', hint: 'Security and API key events are illustrative only.' },
    { label: 'Operational', value: '4', hint: 'Order and position alerts remain adapter snapshots.' },
    { label: 'Last event', value: '2 min ago', hint: 'Based on the local mock clock.' },
  ],
  notifications: [
    {
      id: 'notif-9001',
      title: 'API key created',
      description: 'MM-BOT-01 was created in the development adapter and the secret was shown once.',
      category: 'Security',
      severity: 'success',
      isRead: false,
      createdAt: '2026-06-29T02:14:00Z',
    },
    {
      id: 'notif-9002',
      title: 'Open order partially filled',
      description: 'BTC-USDT limit order reached a partial fill and remains open.',
      category: 'Orders',
      severity: 'info',
      isRead: false,
      createdAt: '2026-06-29T01:41:00Z',
    },
    {
      id: 'notif-9003',
      title: 'Funding payment posted',
      description: 'A synthetic funding payment was recorded for the BTCUSDT perpetual position.',
      category: 'Funding',
      severity: 'warning',
      isRead: true,
      createdAt: '2026-06-29T00:00:00Z',
    },
    {
      id: 'notif-9004',
      title: 'Position risk changed',
      description: 'The mock BTCUSDT-PERP long position moved closer to the target risk band.',
      category: 'Positions',
      severity: 'warning',
      isRead: true,
      createdAt: '2026-06-28T20:18:00Z',
    },
    {
      id: 'notif-9005',
      title: 'System maintenance note',
      description: 'Notification push is not connected to a production queue in this phase.',
      category: 'System',
      severity: 'info',
      isRead: true,
      createdAt: '2026-06-28T11:00:00Z',
    },
  ],
};

export async function fetchOrderCenterMock(): Promise<OrderCenterSnapshot> {
  await delay(360);
  return structuredClone(orderCenterData);
}

export async function fetchPositionCenterMock(): Promise<PositionCenterSnapshot> {
  await delay(360);
  return structuredClone(positionCenterData);
}

export async function fetchApiKeyCenterMock(): Promise<ApiKeySnapshot> {
  await delay(280);
  return structuredClone(apiKeySnapshot);
}

export async function fetchNotificationCenterMock(): Promise<NotificationSnapshot> {
  await delay(300);
  return structuredClone(notificationSnapshot);
}

export function createDevelopmentApiKeyPreview(input: {
  name: string;
  permissions: ApiPermission[];
  ipWhitelist: string;
}): { createdAt: string; secretOnce: string; key: ManagedApiKey } {
  const suffix = Math.random().toString(36).slice(2, 10).toUpperCase();
  const secretOnce = `LXSK.${suffix}.${Math.random().toString(36).slice(2, 10).toUpperCase()}`;
  const createdAt = new Date().toISOString();

  return {
    createdAt,
    secretOnce,
    key: {
      name: input.name,
      status: 'Active',
      permissions: input.permissions,
      ipWhitelist: input.ipWhitelist || '0.0.0.0/0',
      lastUsedAt: createdAt,
      createdAt,
      secretMasked: `${secretOnce.slice(0, 4)}...${secretOnce.slice(-4)}`,
    },
  };
}
