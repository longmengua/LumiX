const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type TradingKind = 'spot' | 'futures' | 'margin';

export type TradingRouteMap = {
  spot: string;
  futures: string;
  margin: string;
};

export type TradingMetric = {
  label: string;
  value: string;
  hint: string;
};

export type TradingOrderBookLevel = {
  price: number;
  size: number;
  total: number;
};

export type TradingFill = {
  time: string;
  side: 'Buy' | 'Sell';
  price: number;
  size: number;
  source: string;
};

export type TradingOpenOrder = {
  id: string;
  side: 'Buy' | 'Sell';
  type: 'Limit' | 'Market' | 'Stop';
  price: number;
  size: number;
  filled: number;
  status: 'Working' | 'Partially Filled' | 'Queued';
};

export type TradingPosition = {
  symbol: string;
  side: 'Long' | 'Short';
  size: number;
  entryPrice: number;
  markPrice: number;
  pnl: number;
  leverage: number;
  marginUsed: number;
  riskRatio: number;
  liqPrice: number;
};

export type TradingBalance = {
  asset: string;
  available: number;
  frozen: number;
  total: number;
  note: string;
};

export type TradingWorkspaceData = {
  kind: TradingKind;
  symbol: string;
  baseAsset: string;
  displaySymbol: string;
  displayName: string;
  adapterNotice: string;
  heroCopy: string;
  midPrice: number;
  change24h: number;
  metrics: TradingMetric[];
  balances: TradingBalance[];
  orderBook: {
    bids: TradingOrderBookLevel[];
    asks: TradingOrderBookLevel[];
  };
  trades: TradingFill[];
  openOrders: TradingOpenOrder[];
  positions: TradingPosition[];
  riskRatio: number;
  fundingOrBorrow: string;
  lastUpdated: string;
  actionLabel: string;
  orderHints: string[];
};

const assetProfiles: Record<
  string,
  {
    spotPrice: number;
    spotChange24h: number;
    spotHigh24h: number;
    spotLow24h: number;
    spotVolume24h: number;
    futuresBasis: number;
    futuresFunding: number;
    marginBorrowRate: number;
  }
> = {
  BTC: {
    spotPrice: 68450.32,
    spotChange24h: 3.42,
    spotHigh24h: 68980.55,
    spotLow24h: 66210.11,
    spotVolume24h: 18250.44,
    futuresBasis: 11.14,
    futuresFunding: 0.018,
    marginBorrowRate: 0.012,
  },
  ETH: {
    spotPrice: 3621.17,
    spotChange24h: 2.18,
    spotHigh24h: 3665.22,
    spotLow24h: 3510.84,
    spotVolume24h: 14620.28,
    futuresBasis: 3.75,
    futuresFunding: 0.011,
    marginBorrowRate: 0.009,
  },
  SOL: {
    spotPrice: 167.45,
    spotChange24h: -1.37,
    spotHigh24h: 173.11,
    spotLow24h: 163.82,
    spotVolume24h: 9341.12,
    futuresBasis: 0.17,
    futuresFunding: -0.006,
    marginBorrowRate: 0.014,
  },
};

const adapterNotice =
  'Development adapter only. OL before must connect server/ Java API, C++ Core event stream, and real WebSocket.';

export function getTradingRoutes(baseAsset: string): TradingRouteMap {
  const normalizedBaseAsset = baseAsset.toUpperCase();
  return {
    spot: `/spot/${normalizedBaseAsset}-USDT`,
    futures: `/futures/${normalizedBaseAsset}USDT-PERP`,
    margin: `/margin/${normalizedBaseAsset}-USDT`,
  };
}

export function extractBaseAsset(symbol: string, kind: TradingKind) {
  const normalized = symbol.toUpperCase();

  if (kind === 'futures') {
    return normalized.replace(/USDT-PERP$/, '').replace(/[^A-Z0-9]/g, '') || 'BTC';
  }

  if (normalized.includes('-')) {
    return normalized.split('-')[0] || 'BTC';
  }

  if (normalized.endsWith('USDT')) {
    return normalized.replace(/USDT$/, '') || 'BTC';
  }

  return normalized || 'BTC';
}

function getAssetProfile(baseAsset: string) {
  return assetProfiles[baseAsset] ?? assetProfiles.BTC;
}

function buildOrderBookLevels(lastPrice: number) {
  const bidSpacing = Math.max(lastPrice * 0.00032, 0.01);
  const askSpacing = Math.max(lastPrice * 0.00035, 0.01);

  const bids = Array.from({ length: 5 }, (_, index) => {
    const depth = index + 1;
    const price = lastPrice - bidSpacing * depth;
    const size = depth * 1.65 + index * 0.22;
    return { price, size, total: price * size };
  });

  const asks = Array.from({ length: 5 }, (_, index) => {
    const depth = index + 1;
    const price = lastPrice + askSpacing * depth;
    const size = depth * 1.48 + index * 0.18;
    return { price, size, total: price * size };
  });

  return { bids, asks };
}

function buildTrades(lastPrice: number, kind: TradingKind): TradingFill[] {
  const offsets = [0.11, -0.05, 0.17, -0.09, 0.03, -0.14];

  return offsets.map((offset, index) => ({
    time: new Date(Date.now() - index * 45_000).toISOString(),
    side: offset >= 0 ? 'Buy' : 'Sell',
    price: lastPrice * (1 + offset / 100),
    size: 0.12 + index * 0.04,
    source:
      kind === 'spot'
        ? index % 2 === 0
          ? 'Aggressive taker'
          : 'Liquidity maker'
        : kind === 'futures'
          ? index % 2 === 0
            ? 'Perp taker'
            : 'Funding sweep'
          : index % 2 === 0
            ? 'Margin fill'
            : 'Borrow re-balance',
  }));
}

function buildOpenOrders(lastPrice: number): TradingOpenOrder[] {
  return [
    {
      id: 'ord-2048',
      side: 'Buy',
      type: 'Limit',
      price: lastPrice * 0.9987,
      size: 0.48,
      filled: 0.14,
      status: 'Partially Filled',
    },
    {
      id: 'ord-2049',
      side: 'Sell',
      type: 'Stop',
      price: lastPrice * 1.0124,
      size: 0.32,
      filled: 0,
      status: 'Working',
    },
    {
      id: 'ord-2050',
      side: 'Buy',
      type: 'Market',
      price: lastPrice * 0.9991,
      size: 0.2,
      filled: 0.2,
      status: 'Queued',
    },
  ];
}

function buildPositions(baseAsset: string, lastPrice: number): TradingPosition[] {
  return [
    {
      symbol: `${baseAsset}USDT-PERP`,
      side: 'Long',
      size: 3.5,
      entryPrice: lastPrice * 0.9918,
      markPrice: lastPrice,
      pnl: lastPrice * 0.0082 * 3.5,
      leverage: 5,
      marginUsed: lastPrice * 0.2,
      riskRatio: 24.8,
      liqPrice: lastPrice * 0.865,
    },
    {
      symbol: `${baseAsset}USDT-PERP`,
      side: 'Short',
      size: 1.2,
      entryPrice: lastPrice * 1.0075,
      markPrice: lastPrice,
      pnl: -lastPrice * 0.0052 * 1.2,
      leverage: 3,
      marginUsed: lastPrice * 0.08,
      riskRatio: 41.7,
      liqPrice: lastPrice * 1.082,
    },
  ];
}

function buildBalances(kind: TradingKind, baseAsset: string): TradingBalance[] {
  if (kind === 'spot') {
    return [
      { asset: 'USDT', available: 18420.53, frozen: 120.4, total: 18540.93, note: 'Spot quote balance' },
      { asset: baseAsset, available: 1.84, frozen: 0.12, total: 1.96, note: 'Base asset inventory' },
    ];
  }

  if (kind === 'futures') {
    return [
      { asset: 'USDT', available: 32450.73, frozen: 860.52, total: 33311.25, note: 'Wallet balance' },
      { asset: baseAsset, available: 0.48, frozen: 0.05, total: 0.53, note: 'Reference collateral' },
    ];
  }

  return [
    { asset: 'USDT', available: 8450.1, frozen: 0, total: 8450.1, note: 'Collateral balance' },
    { asset: baseAsset, available: 0.92, frozen: 0, total: 0.92, note: 'Borrowable reference asset' },
  ];
}

function buildMetrics(kind: TradingKind, profile: ReturnType<typeof getAssetProfile>, lastPrice: number) {
  if (kind === 'spot') {
    return [
      { label: 'Last price', value: `$${lastPrice.toFixed(2)}`, hint: 'Development adapter snapshot only.' },
      { label: '24h change', value: `${profile.spotChange24h.toFixed(2)}%`, hint: 'Mock market change.' },
      { label: '24h high', value: `$${profile.spotHigh24h.toFixed(2)}`, hint: 'Synthetic price ceiling.' },
      { label: '24h volume', value: `$${profile.spotVolume24h.toFixed(2)}K`, hint: 'Local adapter volume estimate.' },
    ];
  }

  if (kind === 'futures') {
    return [
      { label: 'Mark price', value: `$${lastPrice.toFixed(2)}`, hint: 'Derived from the local adapter.' },
      { label: 'Basis', value: `${profile.futuresBasis.toFixed(2)} USDT`, hint: 'Mock perp basis versus spot.' },
      { label: 'Funding', value: `${profile.futuresFunding.toFixed(3)}%`, hint: 'Displayed only, not settled.' },
      { label: '24h change', value: `${(profile.spotChange24h + 0.28).toFixed(2)}%`, hint: 'Synthetic futures move.' },
    ];
  }

  return [
    { label: 'Reference price', value: `$${lastPrice.toFixed(2)}`, hint: 'Spot reference for margin preview.' },
    { label: 'Borrow rate', value: `${profile.marginBorrowRate.toFixed(3)}%`, hint: 'Development-only borrow estimate.' },
    { label: 'Risk ratio', value: '62.4%', hint: 'Illustrative only, not a live liquidation signal.' },
    { label: 'Collateral', value: '$8,450.10', hint: 'Mock isolated margin balance.' },
  ];
}

function buildHeroCopy(kind: TradingKind, baseAsset: string) {
  if (kind === 'spot') {
    return `Spot trade workspace for ${baseAsset}/USDT. The page renders a development adapter only, with book, tape, balances, and preview order entry.`;
  }

  if (kind === 'futures') {
    return `U 本位永續 workspace for ${baseAsset}USDT-PERP. The UI shows a mock perp book, position snapshot, and funding preview without any live matching or settlement.`;
  }

  return `Margin workspace for ${baseAsset}/USDT. The page shows a mock borrow and risk snapshot only; it does not calculate or execute real leverage actions.`;
}

function buildFundingOrBorrow(kind: TradingKind, profile: ReturnType<typeof getAssetProfile>) {
  if (kind === 'spot') {
    return 'No funding or borrow model is active in spot mode.';
  }

  if (kind === 'futures') {
    return `Funding preview: ${profile.futuresFunding.toFixed(3)}% in the adapter snapshot.`;
  }

  return `Borrow preview: ${profile.marginBorrowRate.toFixed(3)}% interest in the adapter snapshot.`;
}

export async function fetchTradingWorkspaceMock(kind: TradingKind, symbol: string): Promise<TradingWorkspaceData> {
  await delay(480);

  const baseAsset = extractBaseAsset(symbol, kind);
  const profile = getAssetProfile(baseAsset);
  const spotPrice = profile.spotPrice;
  const change24h = profile.spotChange24h;
  const lastPrice =
    kind === 'spot'
      ? spotPrice
      : kind === 'futures'
        ? spotPrice + profile.futuresBasis
        : spotPrice * 0.9984;

  const orderBook = buildOrderBookLevels(lastPrice);
  const routes = getTradingRoutes(baseAsset);

  return structuredClone({
    kind,
    symbol: kind === 'futures' ? `${baseAsset}USDT-PERP` : `${baseAsset}-USDT`,
    baseAsset,
    displaySymbol: kind === 'futures' ? `${baseAsset}USDT-PERP` : `${baseAsset}/USDT`,
    displayName:
      kind === 'spot'
        ? `${baseAsset}/USDT Spot`
        : kind === 'futures'
          ? `${baseAsset} USDT Perpetual`
          : `${baseAsset}/USDT Margin`,
    adapterNotice,
    heroCopy: buildHeroCopy(kind, baseAsset),
    midPrice: lastPrice,
    change24h: kind === 'futures' ? change24h + 0.28 : kind === 'margin' ? -0.2 : change24h,
    metrics: buildMetrics(kind, profile, lastPrice),
    balances: buildBalances(kind, baseAsset),
    orderBook,
    trades: buildTrades(lastPrice, kind),
    openOrders: buildOpenOrders(lastPrice),
    positions: kind === 'futures' ? buildPositions(baseAsset, lastPrice) : [],
    riskRatio: kind === 'margin' ? 62.4 : kind === 'futures' ? 31.2 : 0,
    fundingOrBorrow: buildFundingOrBorrow(kind, profile),
    lastUpdated: new Date().toISOString(),
    actionLabel:
      kind === 'spot'
        ? 'Preview buy/sell order'
        : kind === 'futures'
          ? 'Preview perp order'
          : 'Preview margin order',
    orderHints:
      kind === 'spot'
        ? [
            `Route to futures: ${routes.futures}`,
            `Route to margin: ${routes.margin}`,
            'This adapter is view-only and never submits a live order.',
          ]
        : kind === 'futures'
          ? [
              `Route to spot: ${routes.spot}`,
              `Route to margin: ${routes.margin}`,
              'Positions and funding are local snapshots, not live account state.',
            ]
        : [
            `Route to spot: ${routes.spot}`,
            `Route to futures: ${routes.futures}`,
            'Borrow and risk figures are fixed adapter values for Phase 6 UI work.',
          ],
  });
}
