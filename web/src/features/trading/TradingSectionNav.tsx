import { NavLink } from 'react-router-dom';

import { getTradingRoutes, type TradingKind } from './mockTradingService';

type TradingSectionNavProps = {
  kind: TradingKind;
  baseAsset: string;
};

const labels: Record<TradingKind, string> = {
  spot: 'Spot',
  futures: 'Futures',
  margin: 'Margin',
};

export function TradingSectionNav({ kind, baseAsset }: TradingSectionNavProps) {
  const routes = getTradingRoutes(baseAsset);

  return (
    <section className="card trading-section-nav">
      <div>
        <p className="eyebrow">Trading sections</p>
        <p className="trading-section-nav__hint">Development adapter only. These links are local UI routes.</p>
      </div>
      <nav className="tab-list" aria-label="Trading sections">
        {(['spot', 'futures', 'margin'] as const).map((item) => (
          <NavLink
            key={item}
            className={({ isActive }) => `tab-button${isActive || item === kind ? ' tab-button--active' : ''}`}
            to={routes[item]}
          >
            {labels[item]}
          </NavLink>
        ))}
      </nav>
    </section>
  );
}

