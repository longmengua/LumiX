import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { useI18n } from '../i18n';
import {
  FundingTable,
  LiquidationTable,
  PositionTable,
} from '../features/phase7/Phase7Tables';
import {
  fetchPositionCenterMock,
  type PositionCenterSnapshot,
} from '../features/phase7/mockPhase7Service';
import { positionNavItems } from '../features/phase7/positionNav';
import { TradingAdapterNotice } from '../features/trading/TradingAdapterNotice';

export function PositionsPage() {
  const { t } = useI18n();
  const [data, setData] = useState<PositionCenterSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadPositions() {
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchPositionCenterMock();
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load position center.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadPositions();

    return () => {
      alive = false;
    };
  }, []);

  const sidebarItems = useMemo(() => positionNavItems.map(({ to, labelKey }) => ({ to, label: t(labelKey) })), [t]);

  return (
    <div className="two-column positions-page">
      <Sidebar title={t('positions.sidebarTitle')} items={sidebarItems} />
      <div className="stack">
        <PageHeader
          title={t('positions.pageTitle')}
          description={t('positions.pageDescription')}
          actions={
            <div className="hero-actions">
              <NavLink className="secondary-button" to="/orders">
                {t('nav.orders')}
              </NavLink>
              <NavLink className="secondary-button" to="/account/notifications">
                {t('nav.account.notifications')}
              </NavLink>
            </div>
          }
        />

        {loading ? <LoadingState title={t('positions.loadingTitle')} description={t('positions.loadingDescription')} /> : null}
        {error ? <ErrorState title={t('positions.errorTitle')} description={error} /> : null}

        {!loading && !error && data ? (
          <>
            <TradingAdapterNotice notice={data.adapterNotice} />

            <Card title={t('positions.snapshotTitle')}>
              <div className="dashboard-grid dashboard-grid--three">
                {data.summary.map((item) => (
                  <div className="stat-card" key={item.label}>
                    <span className="stat-card__label">{item.label}</span>
                    <strong>{item.value}</strong>
                    <p className="assets-metric__hint">{item.hint}</p>
                  </div>
                ))}
              </div>
            </Card>

            <Routes>
              <Route
                index
                element={
                  <Card title={t('positions.openPositionsTitle')}>
                    <PositionTable positions={data.positions} />
                  </Card>
                }
              />
              <Route
                path="liquidations"
                element={
                  <Card title={t('positions.liquidationsTitle')}>
                    <LiquidationTable records={data.liquidationRecords} />
                  </Card>
                }
              />
              <Route
                path="funding"
                element={
                  <Card title={t('positions.fundingTitle')}>
                    <FundingTable records={data.fundingRecords} />
                  </Card>
                }
              />
              <Route path="*" element={<Navigate replace to="/positions" />} />
            </Routes>

            <Card title={t('positions.guardrailsTitle')}>
              <div className="stack">
                <div className="notice-row">
                  <span>{t('positions.guardrailsNote')}</span>
                </div>
                <div className="phase7-note-grid">
                  <div className="stat-card">
                    <span className="stat-card__label">{t('positions.liveRisk')}</span>
                    <strong>{t('orders.disabled')}</strong>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">{t('positions.liquidationEngine')}</span>
                    <strong>{t('positions.notConnected')}</strong>
                  </div>
                </div>
              </div>
            </Card>
          </>
        ) : null}
      </div>
    </div>
  );
}
