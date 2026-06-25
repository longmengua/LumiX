import { Navigate, Route, Routes } from 'react-router-dom';

import { AppLayout } from '../app/layout';
import { Header } from '../components/layout/Header';
import { HomePage } from '../pages/HomePage';
import { MarketsPage } from '../pages/MarketsPage';
import { PlaceholderTradingPage } from '../pages/PlaceholderTradingPage';
import { AssetsPage } from '../pages/AssetsPage';
import { OrdersPage } from '../pages/OrdersPage';
import { PositionsPage } from '../pages/PositionsPage';
import { AccountPage } from '../pages/AccountPage';
import { AdminPage } from '../pages/AdminPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export function AppRouter() {
  return (
    <Routes>
      <Route
        element={
          <AppLayout header={<Header />}>
            <HomePage />
          </AppLayout>
        }
        path="/"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <MarketsPage />
          </AppLayout>
        }
        path="/markets"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <PlaceholderTradingPage kind="Spot" />
          </AppLayout>
        }
        path="/spot/:symbol"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <PlaceholderTradingPage kind="Futures" />
          </AppLayout>
        }
        path="/futures/:symbol"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <PlaceholderTradingPage kind="Margin" />
          </AppLayout>
        }
        path="/margin/:symbol"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <AssetsPage />
          </AppLayout>
        }
        path="/assets"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <OrdersPage />
          </AppLayout>
        }
        path="/orders"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <PositionsPage />
          </AppLayout>
        }
        path="/positions"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <AccountPage />
          </AppLayout>
        }
        path="/account/*"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <AdminPage />
          </AppLayout>
        }
        path="/admin/*"
      />
      <Route path="/login" element={<Navigate replace to="/" />} />
      <Route path="*" element={<AppLayout header={<Header />}><NotFoundPage /></AppLayout>} />
    </Routes>
  );
}

