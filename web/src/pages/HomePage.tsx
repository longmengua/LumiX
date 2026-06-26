import { NavLink } from 'react-router-dom';

import { Badge } from '../components/base/Badge';
import { Card } from '../components/base/Card';
import { PageHeader } from '../components/layout/PageHeader';
import { formatPercent, formatPrice } from '../utils/format';
import { homeHighlights, homeStats, getInitialMarkets } from '../features/markets/mockMarketService';

export function HomePage() {
  const markets = getInitialMarkets();
  const spotMarkets = markets.filter((market) => market.category === 'spot').slice(0, 3);
  const futuresMarkets = markets.filter((market) => market.category === 'futures').slice(0, 3);

  return (
    <div className="stack">
      <PageHeader
        title="LumiX Exchange"
        description="Frontend root remains React + TypeScript + Vite. The home page now acts as the public launch pad for Phase 3."
        actions={
          <div className="hero-actions">
            <NavLink className="primary-button" to="/login">
              Sign in
            </NavLink>
            <NavLink className="secondary-button" to="/markets">
              Explore markets
            </NavLink>
          </div>
        }
      />

      <section className="hero-panel">
        <div className="hero-panel__copy">
          <p className="eyebrow">Root React shell</p>
          <h2>Trade spot, futures, and margin on a single front door.</h2>
          <p className="lead">
            The public experience is wired for authentication, market discovery, and future trading entry points while the
            Java business backend and C++ Core remain separated by design.
          </p>

          <div className="hero-panel__stats">
            {homeStats.map((item) => (
              <div className="stat-card" key={item.label}>
                <span className="stat-card__label">{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>
        </div>

        <Card title="Phase 3 focus">
          <div className="stack">
            {homeHighlights.map((item) => (
              <article className="link-card" key={item.title}>
                <div>
                  <p className="link-card__title">{item.title}</p>
                  <p className="link-card__description">{item.description}</p>
                </div>
                <NavLink className="link-card__action" to={item.path}>
                  Open
                </NavLink>
              </article>
            ))}
          </div>
        </Card>
      </section>

      <section className="home-grid">
        <Card title="Top spot pairs">
          <div className="market-preview-list">
            {spotMarkets.map((market) => (
              <article className="market-preview" key={market.symbol}>
                <div>
                  <p className="market-preview__symbol">{market.displayName}</p>
                  <p className="market-preview__meta">{market.description}</p>
                </div>
                <div className="market-preview__numbers">
                  <strong>{formatPrice(market.lastPrice, 2)}</strong>
                  <Badge tone={market.change24h >= 0 ? 'success' : 'danger'}>{formatPercent(market.change24h)}</Badge>
                </div>
                <NavLink className="market-preview__action" to={market.tradePath}>
                  Trade
                </NavLink>
              </article>
            ))}
          </div>
        </Card>

        <Card title="Top futures">
          <div className="market-preview-list">
            {futuresMarkets.map((market) => (
              <article className="market-preview" key={market.symbol}>
                <div>
                  <p className="market-preview__symbol">{market.displayName}</p>
                  <p className="market-preview__meta">{market.description}</p>
                </div>
                <div className="market-preview__numbers">
                  <strong>{formatPrice(market.lastPrice, 2)}</strong>
                  <Badge tone={market.change24h >= 0 ? 'success' : 'danger'}>{formatPercent(market.change24h)}</Badge>
                </div>
                <NavLink className="market-preview__action" to={market.tradePath}>
                  Trade
                </NavLink>
              </article>
            ))}
          </div>
        </Card>
      </section>
    </div>
  );
}
