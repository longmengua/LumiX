export const accountNavItems = [
  { to: '/account', labelKey: 'nav.account.overview', end: true },
  { to: '/account/security', labelKey: 'nav.account.security', end: true },
  { to: '/account/kyc', labelKey: 'nav.account.kyc', end: true },
  { to: '/account/assets', labelKey: 'nav.account.assets', end: true },
  { to: '/account/transfer', labelKey: 'nav.account.transfer', end: true },
  { to: '/account/api-keys', labelKey: 'nav.account.apiKeys', end: true },
  { to: '/account/notifications', labelKey: 'nav.account.notifications', end: true },
  { to: '/account/login-history', labelKey: 'nav.account.loginHistory', end: true },
  { to: '/account/security-logs', labelKey: 'nav.account.securityLogs', end: true },
  { to: '/account/preferences', labelKey: 'nav.account.preferences', end: true },
] as const;
