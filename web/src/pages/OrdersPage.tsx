import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { OrderCenterTable, OrderFillTable } from '../features/phase7/Phase7Tables';
import { fetchOrderCenterMock, type OrderCenterSnapshot } from '../features/phase7/mockPhase7Service';
import { orderNavItems } from '../features/phase7/orderNav';
import { TradingAdapterNotice } from '../features/trading/TradingAdapterNotice';
import { formatTime } from '../utils/format';

export function OrdersPage() {
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

  const sidebarItems = useMemo(() => orderNavItems.map(({ to, label }) => ({ to, label })), []);

  return (
    <div className="two-column">
      <Sidebar title="Orders" items={sidebarItems} />
      <div className="stack">
        <PageHeader
          title="Order Center"
          description="Development adapter only. OL before must connect server/ Java order API, C++ Core events, and settlement state."
          actions={
            <div className="hero-actions">
              <NavLink className="secondary-button" to="/positions">
                Positions
              </NavLink>
              <NavLink className="secondary-button" to="/account/api-keys">
                API Keys
              </NavLink>
            </div>
          }
        />

        {loading ? <LoadingState title="Loading order center" description="Fetching the development adapter snapshot..." /> : null}
        {error ? <ErrorState title="Unable to load order center" description={error} /> : null}

        {!loading && !error && data ? (
          <>
            <TradingAdapterNotice notice={data.adapterNotice} />

            <Card title="Order Snapshot">
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
                  <Card title="Open Orders">
                    <OrderCenterTable orders={data.openOrders} />
                  </Card>
                }
              />
              <Route
                path="history"
                element={
                  <Card title="Order History">
                    <OrderCenterTable orders={data.orderHistory} />
                  </Card>
                }
              />
              <Route
                path="fills"
                element={
                  <Card title="Fill History">
                    <OrderFillTable fills={data.fills} />
                  </Card>
                }
              />
              <Route path="*" element={<Navigate replace to="/orders" />} />
            </Routes>

            <Card title="Adapter Guardrails">
              <div className="stack">
                <div className="notice-row">
                  <span>Orders remain a local snapshot until OL wiring lands in `server/` Java and C++ Core settlement events.</span>
                </div>
                <div className="phase7-note-grid">
                  <div className="stat-card">
                    <span className="stat-card__label">Last snapshot</span>
                    <strong>{formatTime(data.openOrders[0]?.updatedAt ?? new Date().toISOString())}</strong>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">Live submission</span>
                    <strong>Disabled</strong>
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

