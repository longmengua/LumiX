import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { useI18n } from '../i18n';
import { OrderCenterTable, OrderFillTable } from '../features/phase7/Phase7Tables';
import { fetchOrderCenterMock, type OrderCenterSnapshot } from '../features/phase7/mockPhase7Service';
import { orderNavItems } from '../features/phase7/orderNav';
import { TradingAdapterNotice } from '../features/trading/TradingAdapterNotice';
import { formatTime } from '../utils/format';

export function OrdersPage() {
  const { t } = useI18n();
  const [data, setData] = useState<OrderCenterSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadOrders() {
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchOrderCenterMock();
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load order center.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadOrders();

    return () => {
      alive = false;
    };
  }, []);

  const sidebarItems = useMemo(() => orderNavItems.map(({ to, labelKey }) => ({ to, label: t(labelKey) })), [t]);

  return (
    <div className="two-column orders-page">
      <Sidebar title={t('orders.sidebarTitle')} items={sidebarItems} />
      <div className="stack">
        <PageHeader
          title={t('orders.pageTitle')}
          description={t('orders.pageDescription')}
          actions={
            <div className="hero-actions">
              <NavLink className="secondary-button" to="/positions">
                {t('nav.positions')}
              </NavLink>
              <NavLink className="secondary-button" to="/account/api-keys">
                {t('nav.account.apiKeys')}
              </NavLink>
            </div>
          }
        />

        {loading ? <LoadingState title={t('orders.loadingTitle')} description={t('orders.loadingDescription')} /> : null}
        {error ? <ErrorState title={t('orders.errorTitle')} description={error} /> : null}

        {!loading && !error && data ? (
          <>
            <TradingAdapterNotice notice={data.adapterNotice} />

            <Card title={t('orders.snapshotTitle')}>
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
                  <Card title={t('orders.openOrdersTitle')}>
                    <OrderCenterTable orders={data.openOrders} />
                  </Card>
                }
              />
              <Route
                path="history"
                element={
                  <Card title={t('orders.historyTitle')}>
                    <OrderCenterTable orders={data.orderHistory} />
                  </Card>
                }
              />
              <Route
                path="fills"
                element={
                  <Card title={t('orders.fillsTitle')}>
                    <OrderFillTable fills={data.fills} />
                  </Card>
                }
              />
              <Route path="*" element={<Navigate replace to="/orders" />} />
            </Routes>

            <Card title={t('orders.guardrailsTitle')}>
              <div className="stack">
                <div className="notice-row">
                  <span>{t('orders.guardrailsNote')}</span>
                </div>
                <div className="phase7-note-grid">
                  <div className="stat-card">
                    <span className="stat-card__label">{t('orders.lastSnapshot')}</span>
                    <strong>{formatTime(data.openOrders[0]?.updatedAt ?? new Date().toISOString())}</strong>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">{t('orders.liveSubmission')}</span>
                    <strong>{t('orders.disabled')}</strong>
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
