import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
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

  const sidebarItems = useMemo(() => positionNavItems.map(({ to, label }) => ({ to, label })), []);

  return (
    <div className="two-column">
      <Sidebar title="Positions" items={sidebarItems} />
      <div className="stack">
        <PageHeader
          title="Position Center"
          description="Development adapter only. OL before must connect server/ Java position API, C++ Core settlement events, and funding / liquidation services."
          actions={
            <div className="hero-actions">
              <NavLink className="secondary-button" to="/orders">
                Orders
              </NavLink>
              <NavLink className="secondary-button" to="/account/notifications">
                Notifications
              </NavLink>
            </div>
          }
        />

        {loading ? <LoadingState title="Loading position center" description="Fetching the development adapter snapshot..." /> : null}
        {error ? <ErrorState title="Unable to load position center" description={error} /> : null}

        {!loading && !error && data ? (
          <>
            <TradingAdapterNotice notice={data.adapterNotice} />

            <Card title="Position Snapshot">
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
                  <Card title="Open Positions">
                    <PositionTable positions={data.positions} />
                  </Card>
                }
              />
              <Route
                path="liquidations"
                element={
                  <Card title="Liquidation Records">
                    <LiquidationTable records={data.liquidationRecords} />
                  </Card>
                }
              />
              <Route
                path="funding"
                element={
                  <Card title="Funding Rate Records">
                    <FundingTable records={data.fundingRecords} />
                  </Card>
                }
              />
              <Route path="*" element={<Navigate replace to="/positions" />} />
            </Routes>

            <Card title="Adapter Guardrails">
              <div className="stack">
                <div className="notice-row">
                  <span>Positions, liquidation, and funding stay adapter-only until OL wiring lands in `server/` Java and C++ Core settlement events.</span>
                </div>
                <div className="phase7-note-grid">
                  <div className="stat-card">
                    <span className="stat-card__label">Live risk</span>
                    <strong>Disabled</strong>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">Liquidation engine</span>
                    <strong>Not connected</strong>
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

