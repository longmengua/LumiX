const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export type WalletAsset = 'USDT' | 'BTC' | 'ETH';
export type WalletNetwork = 'TRC20' | 'ERC20' | 'BTC' | 'SOL';

export type DepositRecord = {
  time: string;
  asset: WalletAsset;
  network: WalletNetwork;
  amount: number;
  txHash: string;
  confirmations: number;
  status: 'Confirmed' | 'Pending' | 'Rejected';
};

export type WithdrawRecord = {
  time: string;
  asset: WalletAsset;
  network: WalletNetwork;
  amount: number;
  fee: number;
  receiveAmount: number;
  address: string;
  txHash: string;
  status: 'Processing' | 'Completed' | 'Rejected';
};

export type WithdrawAddressRecord = {
  id: string;
  label: string;
  asset: WalletAsset;
  network: WalletNetwork;
  address: string;
  memoTag: string;
  whitelistEnabled: boolean;
  riskFlag: 'Low' | 'Medium' | 'High';
  active: boolean;
};

export type WalletNetworkOption = {
  value: WalletNetwork;
  label: string;
  memoRequired: boolean;
};

export type WalletWorkspaceData = {
  adapterNotice: string;
  securityStatus: {
    twoFactor: 'Enabled' | 'Disabled';
    whitelist: 'Enabled' | 'Disabled';
    riskReview: 'Pending' | 'Approved' | 'Manual review';
  };
  deposit: {
    asset: WalletAsset;
    network: WalletNetwork;
    address: string;
    memoTag: string;
    minimumDeposit: number;
    confirmationsRequired: number;
    riskHint: string;
    assets: WalletAsset[];
    networks: WalletNetworkOption[];
    recentDeposits: DepositRecord[];
  };
  withdraw: {
    asset: WalletAsset;
    network: WalletNetwork;
    withdrawAddress: string;
    available: number;
    feeRate: number;
    flatFee: number;
    eta: string;
    riskReview: string;
    securityNote: string;
    assets: WalletAsset[];
    networks: WalletNetworkOption[];
    recentWithdraws: WithdrawRecord[];
  };
  depositHistory: DepositRecord[];
  withdrawHistory: WithdrawRecord[];
  addresses: WithdrawAddressRecord[];
};

const walletData: WalletWorkspaceData = {
  adapterNotice:
    'Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.',
  securityStatus: {
    twoFactor: 'Enabled',
    whitelist: 'Enabled',
    riskReview: 'Manual review',
  },
  deposit: {
    asset: 'USDT',
    network: 'TRC20',
    address: 'TQ9x2m9L2nq7t7A2f3Y9e1WalletDemo8J',
    memoTag: 'Memo required for SOL, optional for TRC20',
    minimumDeposit: 10,
    confirmationsRequired: 2,
    riskHint: 'Only send supported assets to the matching network. Cross-chain deposits are unsupported.',
    assets: ['USDT', 'BTC', 'ETH'],
    networks: [
      { value: 'TRC20', label: 'TRON (TRC20)', memoRequired: false },
      { value: 'ERC20', label: 'Ethereum (ERC20)', memoRequired: false },
      { value: 'SOL', label: 'Solana', memoRequired: true },
    ],
    recentDeposits: [
      {
        time: '2026-06-28T08:12:00Z',
        asset: 'USDT',
        network: 'TRC20',
        amount: 2500,
        txHash: '0x8f12...a9b4',
        confirmations: 3,
        status: 'Confirmed',
      },
      {
        time: '2026-06-27T21:05:00Z',
        asset: 'BTC',
        network: 'BTC',
        amount: 0.35,
        txHash: 'bcrt1q...0f2a',
        confirmations: 1,
        status: 'Pending',
      },
    ],
  },
  withdraw: {
    asset: 'USDT',
    network: 'TRC20',
    withdrawAddress: 'TX7m4g9w6Zq2nR1D8sC8WithdrawDemoQ',
    available: 12480.28,
    feeRate: 0.0015,
    flatFee: 2.5,
    eta: '10 to 30 minutes after risk review and processing',
    riskReview: 'Manual risk review is required for large withdrawals.',
    securityNote: '2FA, whitelist, and risk review must all pass before submission.',
    assets: ['USDT', 'BTC', 'ETH'],
    networks: [
      { value: 'TRC20', label: 'TRON (TRC20)', memoRequired: false },
      { value: 'ERC20', label: 'Ethereum (ERC20)', memoRequired: false },
      { value: 'BTC', label: 'Bitcoin', memoRequired: false },
    ],
    recentWithdraws: [
      {
        time: '2026-06-28T05:20:00Z',
        asset: 'USDT',
        network: 'TRC20',
        amount: 1200,
        fee: 4.3,
        receiveAmount: 1195.7,
        address: 'TX7m4g9w6Zq2nR1D8sC8WithdrawDemoQ',
        txHash: '0x7a55...12cd',
        status: 'Processing',
      },
      {
        time: '2026-06-26T13:11:00Z',
        asset: 'ETH',
        network: 'ERC20',
        amount: 3.2,
        fee: 0.014,
        receiveAmount: 3.186,
        address: '0x1bcd...9e77',
        txHash: '0x9b44...d001',
        status: 'Completed',
      },
    ],
  },
  depositHistory: [
    {
      time: '2026-06-28T08:12:00Z',
      asset: 'USDT',
      network: 'TRC20',
      amount: 2500,
      txHash: '0x8f12...a9b4',
      confirmations: 3,
      status: 'Confirmed',
    },
    {
      time: '2026-06-27T21:05:00Z',
      asset: 'BTC',
      network: 'BTC',
      amount: 0.35,
      txHash: 'bcrt1q...0f2a',
      confirmations: 1,
      status: 'Pending',
    },
    {
      time: '2026-06-25T09:48:00Z',
      asset: 'ETH',
      network: 'ERC20',
      amount: 5.12,
      txHash: '0x1c02...b8f1',
      confirmations: 12,
      status: 'Confirmed',
    },
  ],
  withdrawHistory: [
    {
      time: '2026-06-28T05:20:00Z',
      asset: 'USDT',
      network: 'TRC20',
      amount: 1200,
      fee: 4.3,
      receiveAmount: 1195.7,
      address: 'TX7m4g9w6Zq2nR1D8sC8WithdrawDemoQ',
      txHash: '0x7a55...12cd',
      status: 'Processing',
    },
    {
      time: '2026-06-26T13:11:00Z',
      asset: 'ETH',
      network: 'ERC20',
      amount: 3.2,
      fee: 0.014,
      receiveAmount: 3.186,
      address: '0x1bcd...9e77',
      txHash: '0x9b44...d001',
      status: 'Completed',
    },
    {
      time: '2026-06-22T16:33:00Z',
      asset: 'BTC',
      network: 'BTC',
      amount: 0.18,
      fee: 0.0008,
      receiveAmount: 0.1792,
      address: 'bc1q...0f62',
      txHash: '0x0d55...8c20',
      status: 'Rejected',
    },
  ],
  addresses: [
    {
      id: 'addr-01',
      label: 'Cold Wallet Main',
      asset: 'USDT',
      network: 'TRC20',
      address: 'TX7m4g9w6Zq2nR1D8sC8WithdrawDemoQ',
      memoTag: '',
      whitelistEnabled: true,
      riskFlag: 'Low',
      active: true,
    },
    {
      id: 'addr-02',
      label: 'Treasury Vault',
      asset: 'BTC',
      network: 'BTC',
      address: 'bc1q6n8p4x0m9t2z6l3v5demoaddress',
      memoTag: '',
      whitelistEnabled: true,
      riskFlag: 'Medium',
      active: true,
    },
    {
      id: 'addr-03',
      label: 'External Partner',
      asset: 'ETH',
      network: 'ERC20',
      address: '0x1bcd9f8a11223344556677889900aabbccddeeff',
      memoTag: 'Desk-04',
      whitelistEnabled: false,
      riskFlag: 'High',
      active: false,
    },
  ],
};

export async function fetchWalletWorkspaceMock(): Promise<WalletWorkspaceData> {
  await delay(420);
  return structuredClone(walletData);
}
