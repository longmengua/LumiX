const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type AssetTabKey = 'spot' | 'futures' | 'margin';

export type AssetRow = {
  asset: string;
  available: number;
  frozen: number;
  total: number;
  estimatedValue: number;
  walletBalance?: number;
  marginUsed?: number;
  unrealizedPnl?: number;
  equity?: number;
  borrowed?: number;
  interest?: number;
  netAsset?: number;
  riskRatio?: number;
};

export type AssetAccount = {
  key: AssetTabKey;
  label: string;
  description: string;
  assets: AssetRow[];
};

export type AssetHistoryRecord = {
  time: string;
  account: string;
  asset: string;
  amount: number;
  changeType: string;
  status: 'Completed' | 'Pending' | 'Rejected';
};

export type AssetOverviewData = {
  metrics: Array<{ label: string; value: string; hint: string }>;
  accounts: AssetAccount[];
  history: AssetHistoryRecord[];
  transferBalances: Record<AssetTabKey, Record<string, number>>;
  transferAssets: string[];
};

const assetData: AssetOverviewData = {
  // 這份資產快照只支援頁面顯示與互動測試；真正的資產狀態應由後端與帳本推導。
  metrics: [
    { label: 'Total Equity', value: '$384,250.37', hint: 'Cross-account equity across spot, futures, and margin.' },
    { label: 'Spot Value', value: '$182,400.84', hint: 'Assets settled in the spot wallet.' },
    { label: 'Futures Equity', value: '$124,880.53', hint: 'Wallet balance plus unrealized PnL.' },
    { label: 'Margin Equity', value: '$76,968.99', hint: 'Borrowed assets and interest are isolated.' },
  ],
  accounts: [
    {
      key: 'spot',
      label: 'Spot Account',
      description: 'Balances available for spot trading and transfer only.',
      assets: [
        { asset: 'USDT', available: 12480.28, frozen: 180.4, total: 12660.68, estimatedValue: 12660.68 },
        { asset: 'BTC', available: 1.84, frozen: 0.12, total: 1.96, estimatedValue: 133920.44 },
        { asset: 'ETH', available: 12.5, frozen: 0.8, total: 13.3, estimatedValue: 41420.72 },
      ],
    },
    {
      key: 'futures',
      label: 'Futures Account',
      description: 'Wallet balance, margin used, and unrealized PnL view.',
      assets: [
        {
          asset: 'USDT',
          available: 32450.73,
          frozen: 860.52,
          total: 33311.25,
          estimatedValue: 33311.25,
          walletBalance: 33311.25,
          marginUsed: 22040.11,
          unrealizedPnl: 860.52,
          equity: 33311.25,
        },
        {
          asset: 'BTC',
          available: 0.48,
          frozen: 0.05,
          total: 0.53,
          estimatedValue: 36240.11,
          walletBalance: 0.53,
          marginUsed: 0.31,
          unrealizedPnl: 0.05,
          equity: 0.53,
        },
      ],
    },
    {
      key: 'margin',
      label: 'Margin Account',
      description: 'Borrowed assets, accrued interest, and risk ratio view.',
      assets: [
        {
          asset: 'USDT',
          available: 8450.1,
          frozen: 0,
          total: 8450.1,
          estimatedValue: 21120.5,
          walletBalance: 8450.1,
          borrowed: 6800.05,
          interest: 37.12,
          netAsset: 21120.5,
          riskRatio: 62.4,
        },
        {
          asset: 'BTC',
          available: 0.92,
          frozen: 0,
          total: 0.92,
          estimatedValue: 63020.66,
          walletBalance: 0.92,
          borrowed: 0.16,
          interest: 0.01,
          netAsset: 0.76,
          riskRatio: 44.1,
        },
      ],
    },
  ],
  history: [
    {
      time: '2026-06-26T02:14:00Z',
      account: 'Spot -> Futures',
      asset: 'USDT',
      amount: 1500,
      changeType: 'Transfer',
      status: 'Completed',
    },
    {
      time: '2026-06-25T18:42:00Z',
      account: 'Margin Interest',
      asset: 'USDT',
      amount: 37.12,
      changeType: 'Accrual',
      status: 'Pending',
    },
    {
      time: '2026-06-24T10:05:00Z',
      account: 'Spot Wallet',
      asset: 'BTC',
      amount: 0.5,
      changeType: 'Deposit',
      status: 'Completed',
    },
  ],
  transferBalances: {
    spot: { USDT: 12480.28, BTC: 1.84, ETH: 12.5 },
    futures: { USDT: 32450.73, BTC: 0.48, ETH: 7.2 },
    margin: { USDT: 8450.1, BTC: 0.92, ETH: 3.4 },
  },
  transferAssets: ['USDT', 'BTC', 'ETH'],
};

export async function fetchAssetOverviewMock(): Promise<AssetOverviewData> {
  await delay(420);
  // 以 structuredClone 傳回副本，避免 UI 直接改到共用快照。
  return structuredClone(assetData);
}
