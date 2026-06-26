const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type MarketCategory = 'spot' | 'futures' | 'margin';

export type MarketSnapshot = {
  symbol: string;
  displayName: string;
  category: MarketCategory;
  lastPrice: number;
  change24h: number;
  high24h: number;
  low24h: number;
  volume24h: number;
  tradePath: string;
  description: string;
};

export type HomeHighlight = {
  title: string;
  description: string;
  path: string;
};

const marketSnapshots: MarketSnapshot[] = [
  {
    symbol: 'BTC-USDT',
    displayName: 'BTC/USDT',
    category: 'spot',
    lastPrice: 68450.32,
    change24h: 3.42,
    high24h: 68980.55,
    low24h: 66210.11,
    volume24h: 18250.44,
    tradePath: '/spot/BTC-USDT',
    description: 'Core spot pair with deep liquidity.',
  },
  {
    symbol: 'ETH-USDT',
    displayName: 'ETH/USDT',
    category: 'spot',
    lastPrice: 3621.17,
    change24h: 2.18,
    high24h: 3665.22,
    low24h: 3510.84,
    volume24h: 14620.28,
    tradePath: '/spot/ETH-USDT',
    description: 'High-activity spot market.',
  },
  {
    symbol: 'SOL-USDT',
    displayName: 'SOL/USDT',
    category: 'spot',
    lastPrice: 167.45,
    change24h: -1.37,
    high24h: 173.11,
    low24h: 163.82,
    volume24h: 9341.12,
    tradePath: '/spot/SOL-USDT',
    description: 'Fast-moving altcoin spot pair.',
  },
  {
    symbol: 'BTCUSDT-PERP',
    displayName: 'BTCUSDT Perp',
    category: 'futures',
    lastPrice: 68461.08,
    change24h: 3.9,
    high24h: 68999.95,
    low24h: 66185.4,
    volume24h: 29804.7,
    tradePath: '/futures/BTCUSDT-PERP',
    description: 'U本位永續核心合約。',
  },
  {
    symbol: 'ETHUSDT-PERP',
    displayName: 'ETHUSDT Perp',
    category: 'futures',
    lastPrice: 3624.93,
    change24h: 1.95,
    high24h: 3671.88,
    low24h: 3513.44,
    volume24h: 22560.9,
    tradePath: '/futures/ETHUSDT-PERP',
    description: 'U本位永續深度市場。',
  },
  {
    symbol: 'SOLUSDT-PERP',
    displayName: 'SOLUSDT Perp',
    category: 'futures',
    lastPrice: 167.62,
    change24h: -0.74,
    high24h: 173.32,
    low24h: 163.94,
    volume24h: 10230.41,
    tradePath: '/futures/SOLUSDT-PERP',
    description: 'Momentum driven futures market.',
  },
  {
    symbol: 'BTC-USDT-MARGIN',
    displayName: 'BTC/USDT Margin',
    category: 'margin',
    lastPrice: 68440.21,
    change24h: 3.19,
    high24h: 68934.12,
    low24h: 66202.51,
    volume24h: 7021.77,
    tradePath: '/margin/BTC-USDT',
    description: 'Leverage-enabled spot margin pair.',
  },
  {
    symbol: 'ETH-USDT-MARGIN',
    displayName: 'ETH/USDT Margin',
    category: 'margin',
    lastPrice: 3618.41,
    change24h: 2.01,
    high24h: 3668.03,
    low24h: 3508.26,
    volume24h: 6288.33,
    tradePath: '/margin/ETH-USDT',
    description: 'Margin pair for higher beta flows.',
  },
];

export const homeHighlights: HomeHighlight[] = [
  {
    title: 'Spot liquidity',
    description: 'BTC, ETH, and SOL front page entry points with live market snapshots.',
    path: '/markets?tab=spot',
  },
  {
    title: 'Perp contracts',
    description: 'U 本位永續 markets exposed with futures trade paths and status summaries.',
    path: '/markets?tab=futures',
  },
  {
    title: 'Risk-aware trading',
    description: 'Account, auth, and market navigation prepared for later phases.',
    path: '/login',
  },
];

export const homeStats = [
  { label: '24h volume', value: '$4.8B' },
  { label: 'Active pairs', value: '8' },
  { label: 'Median spread', value: '0.03%' },
  { label: 'System status', value: 'Stable' },
];

export async function fetchMarketsMock() {
  await delay(420);
  return marketSnapshots.map((market) => ({ ...market }));
}

export function getInitialMarkets() {
  return marketSnapshots.map((market) => ({ ...market }));
}
