import { NavLink } from 'react-router-dom';

import { useI18n } from '../../i18n';
import { getTradingRoutes, type TradingKind } from './mockTradingService';

type TradingSectionNavProps = {
  kind: TradingKind;
  baseAsset: string;
};

const labels: Record<TradingKind, string> = {
  spot: 'nav.spot',
  futures: 'nav.futures',
  margin: 'nav.margin',
};

export function TradingSectionNav({ kind, baseAsset }: TradingSectionNavProps) {
  const { t } = useI18n();
  const routes = getTradingRoutes(baseAsset);

  return (
    <section className="card trading-section-nav">
      <div>
        <p className="eyebrow">{t('trading.sections')}</p>
        <p className="trading-section-nav__hint">{t('trading.sectionsHint')}</p>
      </div>
      <nav className="tab-list" aria-label="Trading sections">
        {(['spot', 'futures', 'margin'] as const).map((item) => (
          <NavLink
            key={item}
            className={({ isActive }) => `tab-button${isActive || item === kind ? ' tab-button--active' : ''}`}
            to={routes[item]}
          >
            {t(labels[item])}
          </NavLink>
        ))}
      </nav>
    </section>
  );
}
