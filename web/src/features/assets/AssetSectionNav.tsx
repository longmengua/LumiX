import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

import type { AssetTabKey } from './mockAssetService';

const sectionLinks = [
  { to: '/assets', label: 'Overview' },
  { to: '/assets/spot', label: 'Spot' },
  { to: '/assets/futures', label: 'Futures' },
  { to: '/assets/margin', label: 'Margin' },
  { to: '/assets/transfer', label: 'Transfer' },
] as const;

type AssetSectionNavProps = {
  active?: AssetTabKey;
};

export function AssetSectionNav({ active }: AssetSectionNavProps) {
  return (
    <CardShell title="Account Sections" hint="Separate routes for account details and transfer workstreams.">
      <div className="assets-tabs">
        {sectionLinks.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `tab-button${isActive || activeMatches(active, item.to) ? ' tab-button--active' : ''}`}
            end={item.to === '/assets'}
          >
            {item.label}
          </NavLink>
        ))}
      </div>
    </CardShell>
  );
}

function activeMatches(active: AssetTabKey | undefined, to: string) {
  if (!active) return false;
  if (active === 'spot') return to === '/assets/spot';
  if (active === 'futures') return to === '/assets/futures';
  return to === '/assets/margin';
}

function CardShell({ title, hint, children }: { title: string; hint: string; children: ReactNode }) {
  return (
    <section className="card">
      <h2 className="card__title">{title}</h2>
      <p className="assets-tabs__hint">{hint}</p>
      {children}
    </section>
  );
}
