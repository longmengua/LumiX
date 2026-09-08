import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

import { AppLayout } from '../app/layout';
import { LoadingState } from '../components/base/State';
import { Header } from '../components/layout/Header';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { useI18n } from '../i18n';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { TwoFactorPage } from '../pages/TwoFactorPage';
import { HomePage } from '../pages/HomePage';
import { MarketsPage } from '../pages/MarketsPage';
import { FuturesTradingPage, MarginTradingPage, SpotTradingPage } from '../pages/TradingPage';
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
import { NotFoundPage } from '../pages/NotFoundPage';

const authenticatedPathPrefixes = ['/assets', '/orders', '/positions', '/account', '/change-password'] as const;

function requiresAuthentication(pathname: string) {
  return authenticatedPathPrefixes.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
}

export function AppRouter() {
  const location = useLocation();
  const { loading, user } = useAuthentication();
  const { t } = useI18n();
  const protectedPath = requiresAuthentication(location.pathname);

  // 使用者專屬頁面一律由同一個 session 投影判斷，避免各頁各自實作而遺漏保護。
  if (protectedPath && loading) {
    return (
      <AppLayout header={<Header />}>
        <LoadingState title={t('auth.session.loadingTitle')} description={t('auth.session.loadingDescription')} />
      </AppLayout>
    );
  }

  if (protectedPath && !user) {
    const returnTo = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate replace to="/login" state={{ returnTo }} />;
  }

  // 這份路由只組合頁面層級的導覽；資料讀寫與權限控管要留在對應 feature/service。
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
            <SpotTradingPage />
          </AppLayout>
        }
        path="/spot/:symbol"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <FuturesTradingPage />
          </AppLayout>
        }
        path="/futures/:symbol"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <MarginTradingPage />
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
        path="/orders/*"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <PositionsPage />
          </AppLayout>
        }
        path="/positions/*"
      />
      <Route
        element={
          <AppLayout header={<Header />}>
            <AccountPage />
          </AppLayout>
        }
        path="/account/*"
      />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/change-password" element={<ChangePasswordPage />} />
      <Route path="/two-factor" element={<TwoFactorPage />} />
      <Route path="*" element={<AppLayout header={<Header />}><NotFoundPage /></AppLayout>} />
    </Routes>
  );
}
