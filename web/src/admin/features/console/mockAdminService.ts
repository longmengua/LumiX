const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type AdminSummaryItem = {
  label: string;
  value: string;
  hint: string;
};

export type AdminUserRecord = {
  id: string;
  name: string;
  email: string;
  status: 'Active' | 'Frozen' | 'KYC Pending';
  role: 'Operator' | 'Risk' | 'Support' | 'Auditor';
  kycLevel: string;
  lastLoginAt: string;
  twoFactorState: 'Enabled' | 'Reset pending';
};

export type AdminAssetRecord = {
  asset: string;
  spotBalance: string;
  futuresBalance: string;
  marginBalance: string;
  frozenBalance: string;
  ledgerDelta: string;
};

export type AdminWalletRecord = {
  id: string;
  user: string;
  type: 'Deposit' | 'Withdraw';
  asset: string;
  network: string;
  amount: string;
  status: 'Pending' | 'Approved' | 'Rejected';
  risk: 'Low' | 'Medium' | 'High';
  requestedAt: string;
};

export type AdminSpotRecord = {
  pair: string;
  status: 'Active' | 'Paused' | 'Reduce only';
  volume24h: string;
  feeRate: string;
  orderCount: string;
  actionLabel: string;
};

export type AdminFuturesRecord = {
  symbol: string;
  status: 'Active' | 'Paused' | 'Reduce only';
  openInterest: string;
  fundingRate: string;
  liquidationCount: string;
  markPrice: string;
};

export type AdminMarginRecord = {
  user: string;
  debt: string;
  interest: string;
  borrowStatus: 'Active' | 'Paused';
  riskRatio: string;
};

export type AdminRiskRule = {
  name: string;
  scope: string;
  threshold: string;
  status: 'Enabled' | 'Disabled';
};

export type AdminMarketMakerRecord = {
  name: string;
  apiKey: string;
  status: 'Active' | 'Disabled';
  dailyVolume: string;
  pnl: string;
  lastHeartbeat: string;
};

export type AdminInsuranceFundRecord = {
  currency: string;
  balance: string;
  dailyChange: string;
  lastTransferAt: string;
};

export type AdminReconciliationRecord = {
  date: string;
  status: 'Matched' | 'Investigating' | 'Mismatch';
  matched: string;
  mismatch: string;
  note: string;
};

export type AdminOperationLogRecord = {
  time: string;
  actor: string;
  action: string;
  target: string;
  result: string;
  risk: 'Low' | 'Medium' | 'High';
};

export type AdminSettings = {
  killSwitch: boolean;
  withdrawPause: boolean;
  spotPause: boolean;
  futuresReduceOnly: boolean;
  internalMmStopped: boolean;
  apiWithdrawEnabled: boolean;
  maintenanceWindow: string;
  note: string;
};

export type AdminConsoleSnapshot = {
  adapterNotice: string;
  summary: AdminSummaryItem[];
  users: AdminUserRecord[];
  assets: AdminAssetRecord[];
  wallets: AdminWalletRecord[];
  spotMarkets: AdminSpotRecord[];
  futuresMarkets: AdminFuturesRecord[];
  marginAccounts: AdminMarginRecord[];
  riskRules: AdminRiskRule[];
  marketMakers: AdminMarketMakerRecord[];
  insuranceFund: AdminInsuranceFundRecord[];
  reconciliation: AdminReconciliationRecord[];
  operationLogs: AdminOperationLogRecord[];
  settings: AdminSettings;
};

const snapshot: AdminConsoleSnapshot = {
  adapterNotice:
    'Development adapter only. Phase 8 remains a front-end mock layer until the Java back-office APIs, ledger checks, and operation logs are wired in.',
  summary: [
    { label: 'Total Users', value: '1,284,901', hint: 'Snapshot only, no live auth or KYC sync.' },
    { label: 'Daily Volume', value: '$2.41B', hint: 'Aggregated from mock back-office metrics.' },
    { label: 'Open Withdrawals', value: '18', hint: 'Approval queue rendered from local data.' },
    { label: 'Risk Alerts', value: '7', hint: 'Includes user, market, and limit alerts.' },
    { label: 'Active Market Makers', value: '12', hint: 'API access and heartbeat are simulated.' },
    { label: 'Insurance Fund Balance', value: '$18.2M', hint: 'Displayed for ops review only.' },
    { label: 'Reconciliation Status', value: '1 mismatch', hint: 'Daily settlement diff is awaiting review.' },
    { label: 'System Status', value: 'Degraded', hint: 'Maint. window pending on two admin switches.' },
  ],
  users: [
    {
      id: 'usr-10021',
      name: 'Maya Chen',
      email: 'maya.chen@example.com',
      status: 'Active',
      role: 'Operator',
      kycLevel: 'Advanced',
      lastLoginAt: '2026-07-04T22:10:00Z',
      twoFactorState: 'Enabled',
    },
    {
      id: 'usr-10488',
      name: 'Jared Ng',
      email: 'jared.ng@example.com',
      status: 'Frozen',
      role: 'Support',
      kycLevel: 'Intermediate',
      lastLoginAt: '2026-07-04T15:12:00Z',
      twoFactorState: 'Reset pending',
    },
    {
      id: 'usr-11007',
      name: 'TradeDesk Bot',
      email: 'trade-desk@lumix.exchange',
      status: 'KYC Pending',
      role: 'Auditor',
      kycLevel: 'Pending',
      lastLoginAt: '2026-07-03T09:40:00Z',
      twoFactorState: 'Enabled',
    },
  ],
  assets: [
    { asset: 'USDT', spotBalance: '842,221,440.00', futuresBalance: '211,996,300.00', marginBalance: '88,122,000.00', frozenBalance: '12,050,000.00', ledgerDelta: '+18,240.00' },
    { asset: 'BTC', spotBalance: '21,428.1050', futuresBalance: '8,904.8800', marginBalance: '1,022.4480', frozenBalance: '74.1200', ledgerDelta: '-2.3140' },
    { asset: 'ETH', spotBalance: '154,903.1200', futuresBalance: '62,410.4200', marginBalance: '14,221.0000', frozenBalance: '1,992.0000', ledgerDelta: '+41.2200' },
  ],
  wallets: [
    {
      id: 'wlt-7801',
      user: 'Maya Chen',
      type: 'Withdraw',
      asset: 'USDT',
      network: 'TRC20',
      amount: '12,500.00',
      status: 'Pending',
      risk: 'Low',
      requestedAt: '2026-07-05T01:15:00Z',
    },
    {
      id: 'wlt-7802',
      user: 'Jared Ng',
      type: 'Withdraw',
      asset: 'BTC',
      network: 'BTC',
      amount: '0.8200',
      status: 'Pending',
      risk: 'High',
      requestedAt: '2026-07-05T00:48:00Z',
    },
    {
      id: 'wlt-7803',
      user: 'TradeDesk Bot',
      type: 'Deposit',
      asset: 'ETH',
      network: 'ERC20',
      amount: '320.0000',
      status: 'Approved',
      risk: 'Low',
      requestedAt: '2026-07-04T21:30:00Z',
    },
  ],
  spotMarkets: [
    { pair: 'BTC/USDT', status: 'Active', volume24h: '$812M', feeRate: '0.10%', orderCount: '1.2M', actionLabel: 'Pause' },
    { pair: 'ETH/USDT', status: 'Paused', volume24h: '$406M', feeRate: '0.10%', orderCount: '890k', actionLabel: 'Resume' },
    { pair: 'SOL/USDT', status: 'Reduce only', volume24h: '$95M', feeRate: '0.12%', orderCount: '220k', actionLabel: 'Resume' },
  ],
  futuresMarkets: [
    { symbol: 'BTC-PERP', status: 'Active', openInterest: '$1.8B', fundingRate: '0.008%', liquidationCount: '34', markPrice: '$61,842' },
    { symbol: 'ETH-PERP', status: 'Reduce only', openInterest: '$1.1B', fundingRate: '-0.006%', liquidationCount: '19', markPrice: '$3,438' },
    { symbol: 'SOL-PERP', status: 'Paused', openInterest: '$482M', fundingRate: '0.011%', liquidationCount: '7', markPrice: '$155.22' },
  ],
  marginAccounts: [
    { user: 'Maya Chen', debt: '$38,100', interest: '$42.10', borrowStatus: 'Active', riskRatio: '1.92x' },
    { user: 'Jared Ng', debt: '$12,000', interest: '$18.40', borrowStatus: 'Paused', riskRatio: '2.84x' },
    { user: 'TradeDesk Bot', debt: '$2,200', interest: '$3.11', borrowStatus: 'Active', riskRatio: '1.34x' },
  ],
  riskRules: [
    { name: 'Large withdrawal', scope: 'Wallet', threshold: '> $50,000', status: 'Enabled' },
    { name: 'Cross-market imbalance', scope: 'Market', threshold: '> 18%', status: 'Enabled' },
    { name: 'Margin concentration', scope: 'Risk', threshold: '> 42%', status: 'Disabled' },
  ],
  marketMakers: [
    { name: 'Atlas Liquidity', apiKey: 'mm_atlas_01', status: 'Active', dailyVolume: '$144M', pnl: '+$8,140', lastHeartbeat: '2026-07-05T01:10:00Z' },
    { name: 'North Bridge', apiKey: 'mm_north_02', status: 'Active', dailyVolume: '$121M', pnl: '+$5,220', lastHeartbeat: '2026-07-05T01:06:00Z' },
    { name: 'Blue Delta', apiKey: 'mm_delta_03', status: 'Disabled', dailyVolume: '$0', pnl: '$0', lastHeartbeat: '2026-07-04T23:44:00Z' },
  ],
  insuranceFund: [
    { currency: 'USDT', balance: '$8.6M', dailyChange: '+$24.2k', lastTransferAt: '2026-07-04T18:40:00Z' },
    { currency: 'BTC', balance: '418.23', dailyChange: '+1.21', lastTransferAt: '2026-07-04T11:20:00Z' },
    { currency: 'ETH', balance: '11,920.44', dailyChange: '-8.31', lastTransferAt: '2026-07-03T22:10:00Z' },
  ],
  reconciliation: [
    { date: '2026-07-05', status: 'Investigating', matched: '18,420', mismatch: '1', note: 'One wallet transfer is awaiting operator review.' },
    { date: '2026-07-04', status: 'Matched', matched: '18,322', mismatch: '0', note: 'All ledgers aligned.' },
    { date: '2026-07-03', status: 'Mismatch', matched: '18,301', mismatch: '2', note: 'Two settlement adjustments pending.' },
  ],
  operationLogs: [
    { time: '2026-07-05T01:00:00Z', actor: 'ops-mock-01', action: 'Freeze user', target: 'usr-10488', result: 'Queued', risk: 'Medium' },
    { time: '2026-07-05T00:34:00Z', actor: 'risk-mock-02', action: 'Toggle reduce only', target: 'ETH-PERP', result: 'Queued', risk: 'High' },
    { time: '2026-07-04T23:18:00Z', actor: 'wallet-mock-03', action: 'Approve withdrawal', target: 'wlt-7803', result: 'Approved', risk: 'Low' },
  ],
  settings: {
    killSwitch: false,
    withdrawPause: true,
    spotPause: false,
    futuresReduceOnly: true,
    internalMmStopped: false,
    apiWithdrawEnabled: false,
    maintenanceWindow: '2026-07-06 02:00 UTC - 03:30 UTC',
    note: 'Maintenance note: all toggles are mock-only in Phase 8.',
  },
};

export async function fetchAdminConsoleMock(): Promise<AdminConsoleSnapshot> {
  await delay(220);
  return snapshot;
}
