import { NavLink } from 'react-router-dom';

import { useI18n } from '../../../i18n';

const links = [
  { to: '/assets', labelKey: 'nav.assets.overview' },
  { to: '/assets/spot', labelKey: 'nav.assets.spot' },
  { to: '/assets/futures', labelKey: 'nav.assets.futures' },
  { to: '/assets/margin', labelKey: 'nav.assets.margin' },
  { to: '/assets/transfer', labelKey: 'nav.assets.transfer' },
  { to: '/assets/deposit', labelKey: 'nav.assets.deposit' },
  { to: '/assets/withdraw', labelKey: 'nav.assets.withdraw' },
  { to: '/assets/deposit/history', labelKey: 'nav.assets.depositHistory' },
  { to: '/assets/withdraw/history', labelKey: 'nav.assets.withdrawHistory' },
  { to: '/assets/withdraw/addresses', labelKey: 'nav.assets.withdrawAddresses' },
] as const;

export function WalletSectionNav() {
  const { t } = useI18n();

  return (
    <section className="card">
      <h2 className="card__title">{t('assets.walletSectionTitle')}</h2>
      <p className="assets-tabs__hint">{t('assets.walletWorkspacesHint')}</p>
      <div className="wallet-section-nav">
        {links.map((item) => (
          <NavLink key={item.to} className={({ isActive }) => `tab-button${isActive ? ' tab-button--active' : ''}`} to={item.to} end={item.to === '/assets'}>
            {t(item.labelKey)}
          </NavLink>
        ))}
      </div>
    </section>
  );
}
