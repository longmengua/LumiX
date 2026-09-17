import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

import { useI18n } from '../../i18n';
import type { AssetTabKey } from './assetAccountTypes';

const sectionLinks = [
  { to: '/assets', labelKey: 'nav.assets.overview' },
  { to: '/assets/spot', labelKey: 'nav.assets.spot' },
  { to: '/assets/futures', labelKey: 'nav.assets.futures' },
  { to: '/assets/transfer', labelKey: 'nav.assets.transfer' },
] as const;

type AssetSectionNavProps = {
  active?: AssetTabKey;
  className?: string;
};

export function AssetSectionNav({ active, className }: AssetSectionNavProps) {
  const { t } = useI18n();

  return (
    <CardShell className={className} title={t('assets.walletSectionTitle')} hint={t('assets.walletSectionHint')}>
      <div className="assets-tabs">
        {sectionLinks.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `tab-button${isActive || activeMatches(active, item.to) ? ' tab-button--active' : ''}`}
            end={item.to === '/assets'}
          >
            {t(item.labelKey)}
          </NavLink>
        ))}
      </div>
    </CardShell>
  );
}

function activeMatches(active: AssetTabKey | undefined, to: string) {
  if (!active) return false;
  if (active === 'spot') return to === '/assets/spot';
  return active === 'futures' && to === '/assets/futures';
}

function CardShell({ title, hint, children, className }: { title: string; hint: string; children: ReactNode; className?: string }) {
  return (
    <section className={['card', className].filter(Boolean).join(' ')}>
      <h2 className="card__title">{title}</h2>
      <p className="assets-tabs__hint">{hint}</p>
      {children}
    </section>
  );
}
