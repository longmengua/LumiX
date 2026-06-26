import { useEffect, useMemo, useState } from 'react';
import { NavLink, useSearchParams } from 'react-router-dom';

import { Badge } from '../components/base/Badge';
import { Card } from '../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { fetchMarketsMock, type MarketCategory, type MarketSnapshot } from '../features/markets/mockMarketService';
import { formatAmount, formatPercent, formatPrice } from '../utils/format';

type TabKey = MarketCategory | 'favorites';

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'spot', label: 'Spot' },
  { key: 'futures', label: 'Futures' },
  { key: 'margin', label: 'Margin' },
  { key: 'favorites', label: 'Favorites' },
];

export function MarketsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [markets, setMarkets] = useState<MarketSnapshot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [favoriteSymbols, setFavoriteSymbols] = useState<Set<string>>(new Set(['BTC-USDT']));
  const [query, setQuery] = useState('');

  const activeTab = (searchParams.get('tab') as TabKey | null) ?? 'spot';
  const simulateError = searchParams.get('mockError') === '1';

  useEffect(() => {
    let alive = true;

    async function loadMarkets() {
      setLoading(true);
      setError(null);

      try {
        if (simulateError) {
          throw new Error('Market snapshot failed to load.');
        }

        const snapshot = await fetchMarketsMock();

        if (alive) {
          setMarkets(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load markets.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadMarkets();

    return () => {
      alive = false;
    };
  }, [simulateError]);

  const filteredMarkets = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return markets.filter((market) => {
      const matchesTab =
        activeTab === 'favorites'
          ? favoriteSymbols.has(market.symbol)
          : market.category === activeTab;
      const matchesQuery =
        !normalizedQuery ||
        market.symbol.toLowerCase().includes(normalizedQuery) ||
        market.displayName.toLowerCase().includes(normalizedQuery);

      return matchesTab && matchesQuery;
    });
  }, [activeTab, favoriteSymbols, markets, query]);

  function toggleFavorite(symbol: string) {
    setFavoriteSymbols((current) => {
      const next = new Set(current);
      if (next.has(symbol)) {
        next.delete(symbol);
      } else {
        next.add(symbol);
      }
      return next;
    });
  }

  function switchTab(tab: TabKey) {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('tab', tab);
    setSearchParams(nextParams, { replace: true });
  }

  return (
    <div className="stack">
      <PageHeader
        title="Markets"
        description="Spot, futures, margin, and favorites are filtered locally while the mock market service simulates loading states."
        actions={
          <label className="market-search">
            <span className="sr-only">Search markets</span>
            <input
              className="input"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search symbol"
            />
          </label>
        }
      />

      <Card>
        <div className="tab-list" role="tablist" aria-label="Market categories">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`tab-button${activeTab === tab.key ? ' tab-button--active' : ''}`}
              type="button"
              onClick={() => switchTab(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </Card>

      {loading ? <LoadingState title="Loading markets" description="Fetching mock market snapshots..." /> : null}
      {error ? <ErrorState title="Market load failed" description={error} action={<NavLink to="/markets">Retry</NavLink>} /> : null}

      {!loading && !error && filteredMarkets.length === 0 ? (
        <EmptyState title="No markets found" description="Try a different tab or search term." />
      ) : null}

      {!loading && !error && filteredMarkets.length > 0 ? (
        <Card title="Live market snapshot">
          <div className="market-table">
            <div className="market-table__head">
              <span>Symbol</span>
              <span>Last Price</span>
              <span>24h Change</span>
              <span>24h High</span>
              <span>24h Low</span>
              <span>24h Volume</span>
              <span>Action</span>
            </div>

            {filteredMarkets.map((market) => (
              <article className="market-table__row" key={market.symbol}>
                <div>
                  <div className="market-row__title">
                    <strong>{market.displayName}</strong>
                    <Badge tone={favoriteSymbols.has(market.symbol) ? 'warning' : 'neutral'}>
                      {favoriteSymbols.has(market.symbol) ? 'Favorite' : market.category}
                    </Badge>
                  </div>
                  <p className="market-row__meta">{market.description}</p>
                </div>

                <span>{formatPrice(market.lastPrice, 2)}</span>
                <span className={market.change24h >= 0 ? 'pnl--positive' : 'pnl--negative'}>
                  {formatPercent(market.change24h)}
                </span>
                <span>{formatPrice(market.high24h, 2)}</span>
                <span>{formatPrice(market.low24h, 2)}</span>
                <span>{formatAmount(market.volume24h, 2)}</span>

                <div className="market-row__actions">
                  <button className="ghost-button" type="button" onClick={() => toggleFavorite(market.symbol)}>
                    {favoriteSymbols.has(market.symbol) ? 'Unfavorite' : 'Favorite'}
                  </button>
                  <NavLink className="primary-button" to={market.tradePath}>
                    Trade
                  </NavLink>
                </div>
              </article>
            ))}
          </div>
        </Card>
      ) : null}
    </div>
  );
}
