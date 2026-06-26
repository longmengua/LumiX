const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type KycStatus = 'unverified' | 'pending' | 'verified' | 'rejected' | 'need_more_info';

export type AccountProfile = {
  uid: string;
  email: string;
  phone: string;
  registeredAt: string;
  securityLevel: string;
  kycStatus: KycStatus;
  kycLevel: string;
  kycNotes: string;
  totalEquity: number;
  spotValue: number;
  futuresValue: number;
  marginValue: number;
  riskWarnings: string[];
};

export type SecurityItem = {
  label: string;
  status: string;
  description: string;
};

export type AssetAccountSummary = {
  accountType: 'Spot Account' | 'Futures Account' | 'Margin Account';
  assets: Array<{
    asset: string;
    available: number;
    frozen: number;
    marginUsed: number;
    debt: number;
    interest: number;
    equity: number;
    estimatedValue: number;
  }>;
};

export type ApiKeyRecord = {
  name: string;
  secretMasked: string;
  permissions: string[];
  ipWhitelist: string;
  lastUsedAt: string;
  status: 'Active' | 'Paused';
};

export type TimelineItem = {
  title: string;
  description: string;
  createdAt: string;
};

export type PreferenceItem = {
  label: string;
  value: string;
};

export type AccountDashboardData = {
  profile: AccountProfile;
  securityItems: SecurityItem[];
  assetAccounts: AssetAccountSummary[];
  apiKeys: ApiKeyRecord[];
  notifications: TimelineItem[];
  loginHistory: TimelineItem[];
  securityLogs: TimelineItem[];
  preferences: PreferenceItem[];
};

const accountData: AccountDashboardData = {
  profile: {
    uid: 'LX-20240618-0007',
    email: 'trade.secure@lumix.exchange',
    phone: '+886 912 345 678',
    registeredAt: '2026-04-12T09:24:00Z',
    securityLevel: 'Advanced',
    kycStatus: 'verified',
    kycLevel: 'Level 2',
    kycNotes: 'Withdrawals and futures enabled. Additional limits available after review.',
    totalEquity: 384250.37,
    spotValue: 182400.84,
    futuresValue: 124880.53,
    marginValue: 76968.99,
    riskWarnings: ['Withdrawal whitelist enabled', '2FA active', 'API withdraw permission disabled'],
  },
  securityItems: [
    { label: 'Login Password', status: 'Updated', description: 'Last changed 23 days ago' },
    { label: 'Google Authenticator', status: 'Enabled', description: 'Required for withdrawals and risk actions' },
    { label: 'Email Verification', status: 'Enabled', description: 'Used for sensitive actions' },
    { label: 'SMS Verification', status: 'Enabled', description: 'Backup verification channel' },
    { label: 'Withdrawal Whitelist', status: 'Enabled', description: 'Only approved addresses allowed' },
    { label: 'Device Management', status: '4 devices', description: 'Last login from Taipei, Taiwan' },
    { label: 'Security Activity', status: 'Live', description: 'All critical actions are audited' },
  ],
  assetAccounts: [
    {
      accountType: 'Spot Account',
      assets: [
        { asset: 'USDT', available: 12480.28, frozen: 180.4, marginUsed: 0, debt: 0, interest: 0, equity: 12660.68, estimatedValue: 12660.68 },
        { asset: 'BTC', available: 1.84, frozen: 0.12, marginUsed: 0, debt: 0, interest: 0, equity: 1.96, estimatedValue: 133920.44 },
      ],
    },
    {
      accountType: 'Futures Account',
      assets: [
        { asset: 'USDT', available: 32450.73, frozen: 860.52, marginUsed: 22040.11, debt: 0, interest: 0, equity: 33311.25, estimatedValue: 33311.25 },
      ],
    },
    {
      accountType: 'Margin Account',
      assets: [
        { asset: 'USDT', available: 8450.1, frozen: 0, marginUsed: 21120.45, debt: 6800.05, interest: 37.12, equity: 21120.5, estimatedValue: 21120.5 },
      ],
    },
  ],
  apiKeys: [
    {
      name: 'MM-BOT-01',
      secretMasked: 'LXAK...8C2F',
      permissions: ['read', 'spot trade'],
      ipWhitelist: '203.0.113.10, 203.0.113.11',
      lastUsedAt: '2026-06-26T03:21:00Z',
      status: 'Active',
    },
    {
      name: 'Admin-ReadOnly',
      secretMasked: 'LXAK...19AA',
      permissions: ['read'],
      ipWhitelist: '192.0.2.34',
      lastUsedAt: '2026-06-24T11:55:00Z',
      status: 'Paused',
    },
  ],
  notifications: [
    {
      title: 'Withdrawal address approved',
      description: 'A new whitelist address was approved after security verification.',
      createdAt: '2026-06-26T02:10:00Z',
    },
    {
      title: 'Futures position updated',
      description: 'Your BTCUSDT perpetual position margin ratio changed.',
      createdAt: '2026-06-25T18:35:00Z',
    },
  ],
  loginHistory: [
    {
      title: 'Web login',
      description: 'Chrome on macOS, Taipei, Taiwan',
      createdAt: '2026-06-26T08:42:00Z',
    },
    {
      title: 'Mobile login',
      description: 'iPhone Safari, Kaohsiung, Taiwan',
      createdAt: '2026-06-25T21:11:00Z',
    },
  ],
  securityLogs: [
    {
      title: 'API key created',
      description: 'MM-BOT-01 created through security verification modal.',
      createdAt: '2026-06-24T08:02:00Z',
    },
    {
      title: 'Withdrawal whitelist updated',
      description: 'Address book updated with cold wallet destination.',
      createdAt: '2026-06-23T13:22:00Z',
    },
  ],
  preferences: [
    { label: 'Language', value: '繁體中文' },
    { label: 'Theme', value: 'Dark / high contrast' },
    { label: 'Price precision', value: 'Auto' },
    { label: 'Email notifications', value: 'Enabled' },
    { label: 'App notifications', value: 'Enabled' },
  ],
};

export async function fetchAccountDashboardMock(): Promise<AccountDashboardData> {
  await delay(420);
  return structuredClone(accountData);
}
