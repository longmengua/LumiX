import { Route, Routes } from 'react-router-dom';

import { AppLayout } from '../app/layout';
import { Header } from '../components/layout/Header';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { TwoFactorPage } from '../pages/TwoFactorPage';
import { HomePage } from '../pages/HomePage';
import { MarketsPage } from '../pages/MarketsPage';
import { PlaceholderTradingPage } from '../pages/PlaceholderTradingPage';
import { AssetsOverviewPage } from '../pages/assets/AssetsOverviewPage';
import { SpotAssetsPage } from '../pages/assets/SpotAssetsPage';
import { FuturesAssetsPage } from '../pages/assets/FuturesAssetsPage';
import { MarginAssetsPage } from '../pages/assets/MarginAssetsPage';
import { TransferAssetsPage } from '../pages/assets/TransferAssetsPage';
import { DepositPage } from '../pages/assets/DepositPage';
import { WithdrawPage } from '../pages/assets/WithdrawPage';
import { DepositHistoryPage } from '../pages/assets/DepositHistoryPage';
import { WithdrawHistoryPage } from '../pages/assets/WithdrawHistoryPage';
import { WithdrawAddressesPage } from '../pages/assets/WithdrawAddressesPage';
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
            <AssetsOverviewPage />
          </AppLayout>
        }
        path="/assets"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <SpotAssetsPage />
          </AppLayout>
        }
        path="/assets/spot"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <FuturesAssetsPage />
          </AppLayout>
        }
        path="/assets/futures"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <MarginAssetsPage />
          </AppLayout>
        }
        path="/assets/margin"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <TransferAssetsPage />
          </AppLayout>
        }
        path="/assets/transfer"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <DepositPage />
          </AppLayout>
        }
        path="/assets/deposit"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <WithdrawPage />
          </AppLayout>
        }
        path="/assets/withdraw"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <DepositHistoryPage />
          </AppLayout>
        }
        path="/assets/deposit/history"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <WithdrawHistoryPage />
          </AppLayout>
        }
        path="/assets/withdraw/history"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <WithdrawAddressesPage />
          </AppLayout>
        }
        path="/assets/withdraw/addresses"
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
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/two-factor" element={<TwoFactorPage />} />
      <Route path="*" element={<AppLayout header={<Header />}><NotFoundPage /></AppLayout>} />
    </Routes>
  );
}
