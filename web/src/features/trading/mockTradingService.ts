const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type TradingKind = 'spot' | 'futures' | 'margin';

export type TradingRouteMap = {
  spot: string;
  futures: string;
  margin: string;
};

export type TradingCopy = {
  key: string;
  values?: Record<string, string | number>;
};

export type TradingMetric = {
  labelKey: string;
  value: string;
  hintKey: string;
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
  sourceKey: string;
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
  noteKey: string;
};

export type TradingWorkspaceData = {
  kind: TradingKind;
  symbol: string;
  baseAsset: string;
  displaySymbol: string;
  displayNameKey: string;
  displayNameValues: Record<string, string | number>;
  adapterNoticeKey: string;
  heroCopyKey: string;
  heroCopyValues: Record<string, string | number>;
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
  fundingOrBorrowKey: string;
  fundingOrBorrowValues?: Record<string, string | number>;
  lastUpdated: string;
  actionLabelKey: string;
  orderHintCopies: TradingCopy[];
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

export function getTradingRoutes(baseAsset: string): TradingRouteMap {
  // 路由只負責示意市場入口，真正的交易上下文仍要由 workspace data 提供。
  const normalizedBaseAsset = baseAsset.toUpperCase();
  return {
    spot: `/spot/${normalizedBaseAsset}-USDT`,
    futures: `/futures/${normalizedBaseAsset}USDT-PERP`,
    margin: `/margin/${normalizedBaseAsset}-USDT`,
  };
}

export function extractBaseAsset(symbol: string, kind: TradingKind) {
  // 這裡只做 symbol 正規化，避免 UI 因不同商品格式而拆出多份邏輯。
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
  // 這些深度資料是為了版面與互動測試，不是可交易的市場快照。
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
  // 成交明細只做時間序列演示，不能拿來當作實際成交證明。
  const offsets = [0.11, -0.05, 0.17, -0.09, 0.03, -0.14];

  return offsets.map((offset, index) => ({
    time: new Date(Date.now() - index * 45_000).toISOString(),
    side: offset >= 0 ? 'Buy' : 'Sell',
    price: lastPrice * (1 + offset / 100),
    size: 0.12 + index * 0.04,
    sourceKey:
      kind === 'spot'
        ? index % 2 === 0
          ? 'trading.tradeFeed.source.aggressiveTaker'
          : 'trading.tradeFeed.source.liquidityMaker'
        : kind === 'futures'
          ? index % 2 === 0
            ? 'trading.tradeFeed.source.perpTaker'
            : 'trading.tradeFeed.source.fundingSweep'
          : index % 2 === 0
            ? 'trading.tradeFeed.source.marginFill'
            : 'trading.tradeFeed.source.borrowRebalance',
  }));
}

function buildOpenOrders(lastPrice: number): TradingOpenOrder[] {
  // 未完成委託只用來測試表格與狀態切換，實際委託仍要由 API 與撮合結果決定。
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
  // 部位資料偏重風險與 UI 呈現，不代表真實持倉計算已完成。
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
      { asset: 'USDT', available: 18420.53, frozen: 120.4, total: 18540.93, noteKey: 'trading.balance.note.spotQuote' },
      { asset: baseAsset, available: 1.84, frozen: 0.12, total: 1.96, noteKey: 'trading.balance.note.baseInventory' },
    ];
  }

  if (kind === 'futures') {
    return [
      { asset: 'USDT', available: 32450.73, frozen: 860.52, total: 33311.25, noteKey: 'trading.balance.note.walletBalance' },
      { asset: baseAsset, available: 0.48, frozen: 0.05, total: 0.53, noteKey: 'trading.balance.note.referenceCollateral' },
    ];
  }

  return [
    { asset: 'USDT', available: 8450.1, frozen: 0, total: 8450.1, noteKey: 'trading.balance.note.collateral' },
    { asset: baseAsset, available: 0.92, frozen: 0, total: 0.92, noteKey: 'trading.balance.note.borrowableReference' },
  ];
}

function buildMetrics(kind: TradingKind, profile: ReturnType<typeof getAssetProfile>, lastPrice: number) {
  if (kind === 'spot') {
    return [
      { labelKey: 'trading.metric.lastPrice', value: `$${lastPrice.toFixed(2)}`, hintKey: 'trading.metric.spotLastPriceHint' },
      { labelKey: 'trading.metric.change24h', value: `${profile.spotChange24h.toFixed(2)}%`, hintKey: 'trading.metric.mockMarketChangeHint' },
      { labelKey: 'trading.metric.high24h', value: `$${profile.spotHigh24h.toFixed(2)}`, hintKey: 'trading.metric.syntheticCeilingHint' },
      { labelKey: 'trading.metric.volume24h', value: `$${profile.spotVolume24h.toFixed(2)}K`, hintKey: 'trading.metric.volumeEstimateHint' },
    ];
  }

  if (kind === 'futures') {
    return [
      { labelKey: 'trading.metric.markPrice', value: `$${lastPrice.toFixed(2)}`, hintKey: 'trading.metric.markPriceHint' },
      { labelKey: 'trading.metric.basis', value: `${profile.futuresBasis.toFixed(2)} USDT`, hintKey: 'trading.metric.mockPerpBasisHint' },
      { labelKey: 'trading.metric.funding', value: `${profile.futuresFunding.toFixed(3)}%`, hintKey: 'trading.metric.fundingDisplayedOnlyHint' },
      { labelKey: 'trading.metric.change24h', value: `${(profile.spotChange24h + 0.28).toFixed(2)}%`, hintKey: 'trading.metric.syntheticFuturesMoveHint' },
    ];
  }

  return [
    { labelKey: 'trading.metric.referencePrice', value: `$${lastPrice.toFixed(2)}`, hintKey: 'trading.metric.spotReferenceHint' },
    { labelKey: 'trading.metric.borrowRate', value: `${profile.marginBorrowRate.toFixed(3)}%`, hintKey: 'trading.metric.borrowEstimateHint' },
    { labelKey: 'trading.metric.riskRatio', value: '62.4%', hintKey: 'trading.metric.liveLiquidationHint' },
    { labelKey: 'trading.metric.collateral', value: '$8,450.10', hintKey: 'trading.metric.isolatedMarginHint' },
  ];
}

function buildHeroCopy(kind: TradingKind, baseAsset: string) {
  if (kind === 'spot') {
    return { key: 'trading.instrument.spotSubtitle', values: { symbol: `${baseAsset}/USDT` } };
  }

  if (kind === 'futures') {
    return { key: 'trading.instrument.futuresSubtitle', values: { symbol: `${baseAsset}USDT-PERP` } };
  }

  return { key: 'trading.instrument.marginSubtitle', values: { symbol: `${baseAsset}/USDT` } };
}

function buildFundingOrBorrow(kind: TradingKind, profile: ReturnType<typeof getAssetProfile>) {
  if (kind === 'spot') {
    return { key: 'trading.instrument.noFundingOrBorrow' };
  }

  if (kind === 'futures') {
    return { key: 'trading.instrument.fundingPreview', values: { rate: profile.futuresFunding.toFixed(3) } };
  }

  return { key: 'trading.instrument.borrowPreview', values: { rate: profile.marginBorrowRate.toFixed(3) } };
}

export async function fetchTradingWorkspaceMock(kind: TradingKind, symbol: string): Promise<TradingWorkspaceData> {
  await delay(480);

  const baseAsset = extractBaseAsset(symbol, kind);
  const profile = getAssetProfile(baseAsset);
  const heroCopy = buildHeroCopy(kind, baseAsset);
  const fundingOrBorrow = buildFundingOrBorrow(kind, profile);
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
    displayNameKey:
      kind === 'spot'
        ? 'trading.instrument.spotTitle'
        : kind === 'futures'
          ? 'trading.instrument.futuresTitle'
          : 'trading.instrument.marginTitle',
    displayNameValues: { symbol: kind === 'futures' ? `${baseAsset}USDT-PERP` : `${baseAsset}/USDT` },
    adapterNoticeKey: 'trading.developmentNotice.description',
    heroCopyKey: heroCopy.key,
    heroCopyValues: heroCopy.values ?? {},
    midPrice: lastPrice,
    change24h: kind === 'futures' ? change24h + 0.28 : kind === 'margin' ? -0.2 : change24h,
    metrics: buildMetrics(kind, profile, lastPrice),
    balances: buildBalances(kind, baseAsset),
    orderBook,
    trades: buildTrades(lastPrice, kind),
    openOrders: buildOpenOrders(lastPrice),
    positions: kind === 'futures' ? buildPositions(baseAsset, lastPrice) : [],
    riskRatio: kind === 'margin' ? 62.4 : kind === 'futures' ? 31.2 : 0,
    fundingOrBorrowKey: fundingOrBorrow.key,
    fundingOrBorrowValues: fundingOrBorrow.values,
    lastUpdated: new Date().toISOString(),
    actionLabelKey:
      kind === 'spot'
        ? 'trading.orderEntry.previewSpotOrder'
        : kind === 'futures'
          ? 'trading.orderEntry.previewPerpOrder'
          : 'trading.orderEntry.previewMarginOrder',
    orderHintCopies:
      kind === 'spot'
        ? [
            { key: 'trading.orderHint.routeToFutures', values: { route: routes.futures } },
            { key: 'trading.orderHint.routeToMargin', values: { route: routes.margin } },
            { key: 'trading.orderHint.viewOnly' },
          ]
        : kind === 'futures'
          ? [
              { key: 'trading.orderHint.routeToSpot', values: { route: routes.spot } },
              { key: 'trading.orderHint.routeToMargin', values: { route: routes.margin } },
              { key: 'trading.orderHint.localSnapshotsOnly' },
            ]
        : [
            { key: 'trading.orderHint.routeToSpot', values: { route: routes.spot } },
            { key: 'trading.orderHint.routeToFutures', values: { route: routes.futures } },
            { key: 'trading.orderHint.fixedAdapterValues' },
          ],
  });
}
