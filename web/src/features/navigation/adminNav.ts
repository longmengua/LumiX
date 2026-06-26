export const adminNavItems = [
  { to: '/admin', label: 'Dashboard' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/assets', label: 'Assets' },
  { to: '/admin/wallet', label: 'Wallet' },
  { to: '/admin/spot', label: 'Spot' },
  { to: '/admin/futures', label: 'Futures' },
  { to: '/admin/margin', label: 'Margin' },
  { to: '/admin/risk', label: 'Risk' },
  { to: '/admin/market-makers', label: 'Market Makers' },
  { to: '/admin/insurance-fund', label: 'Insurance Fund' },
  { to: '/admin/reconciliation', label: 'Reconciliation' },
  { to: '/admin/operation-logs', label: 'Operation Logs' },
  { to: '/admin/settings', label: 'Settings' },
] as const;

