import { NavLink } from 'react-router-dom';

import { Badge } from '../components/base/Badge';
import { Card } from '../components/base/Card';
import { PageHeader } from '../components/layout/PageHeader';
import { useI18n } from '../i18n';
import { formatPercent, formatPrice } from '../utils/format';
import { homeHighlights, homeStats, getInitialMarkets } from '../features/markets/mockMarketService';

export function HomePage() {
  const { t } = useI18n();
  const markets = getInitialMarkets();
  const spotMarkets = markets.filter((market) => market.category === 'spot').slice(0, 3);
  const futuresMarkets = markets.filter((market) => market.category === 'futures').slice(0, 3);

  return (
    <div className="stack home-page">
      <PageHeader
        title={t('home.title')}
        description={t('home.description')}
        actions={
          <div className="hero-actions">
            <NavLink className="primary-button" to="/login">
              {t('home.signIn')}
            </NavLink>
            <NavLink className="secondary-button" to="/markets">
              {t('home.exploreMarkets')}
            </NavLink>
          </div>
        }
      />

      <section className="hero-panel home-highlight-grid">
        <div className="hero-panel__copy">
          <p className="eyebrow">{t('home.eyebrow')}</p>
          <h2>{t('home.heroTitle')}</h2>
          <p className="lead">{t('home.heroLead')}</p>

          <div className="hero-panel__stats">
            {homeStats.map((item) => (
              <div className="stat-card" key={item.label}>
                <span className="stat-card__label">{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>
        </div>

        <Card title={t('home.phaseFocus')}>
          <div className="stack">
            {homeHighlights.map((item) => (
              <article className="link-card" key={item.title}>
                <div>
                  <p className="link-card__title">{item.title}</p>
                  <p className="link-card__description">{item.description}</p>
                </div>
                <NavLink className="link-card__action" to={item.path}>
                  {t('home.open')}
                </NavLink>
              </article>
            ))}
          </div>
        </Card>
      </section>

      <section className="home-grid home-market-grid">
        <Card title={t('home.topSpotPairs')}>
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
                  {t('home.trade')}
                </NavLink>
              </article>
            ))}
          </div>
        </Card>

        <Card title={t('home.topFutures')}>
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
                  {t('home.trade')}
                </NavLink>
              </article>
            ))}
          </div>
        </Card>
      </section>
    </div>
  );
}
