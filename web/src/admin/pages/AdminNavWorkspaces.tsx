import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';

import { Card } from '../../components/base/Card';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { SpotAssetConfigurationPanel } from '../features/assets/SpotAssetConfigurationPanel';
import { AdminUsersPage } from '../features/users/AdminUsersPage';

type WorkspaceTab = { to: string; label: string; content?: ReactNode };

/**
 * 頂部導覽之下統一保留可分享的左側工作台頁籤；尚未具備後端契約的區塊明確顯示不可用，
 * 避免沿用舊 AdminConsole 的示意快照而讓管理員誤判為真實營運資料。
 */
function AdminNavWorkspace({ title, tabs }: { title: string; tabs: WorkspaceTab[] }) {
  const { t } = useI18n();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const normalizedPath = pathname.replace(/\/+$/, '');
  const activeTab = tabs.find((tab) => normalizedPath === tab.to || normalizedPath.startsWith(`${tab.to}/`)) ?? tabs[0];
  const isRootPath = tabs.every((tab) => normalizedPath !== tab.to && !normalizedPath.startsWith(`${tab.to}/`));

  if (isRootPath) return <Navigate replace to={tabs[0].to} />;

  return (
    <div className="stack">
      <PageHeader title={title} description={t('admin.workspaceDescription')} />
      <div className="admin-assets-workspace">
        <aside className="admin-assets-workspace__sidebar" aria-label={t('admin.workspaceTabs')}>
          {tabs.map((tab) => <button className={`admin-assets-workspace__tab${activeTab.to === tab.to ? ' admin-assets-workspace__tab--active' : ''}`} key={tab.to} type="button" onClick={() => navigate(tab.to)}>{tab.label}</button>)}
        </aside>
        <div className="admin-assets-workspace__content">
          {activeTab.content ?? <Card title={t('admin.workspaceUnavailableTitle')}><p className="assets-metric__hint">{t('admin.workspaceUnavailableDescription')}</p></Card>}
        </div>
      </div>
    </div>
  );
}

export function AdminUsersWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.users')} tabs={[{ to: '/users/list', label: t('admin.nav.users'), content: <AdminUsersPage /> }]} />;
}

export function AdminActivitiesWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.activities')} tabs={[{ to: '/activities/configuration', label: t('admin.nav.activities') }]} />;
}

export function AdminWalletWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.wallet')} tabs={[
    { to: '/wallet/asset-networks', label: t('admin.walletAssetNetworksTab'), content: <Card title={t('admin.walletDepositWithdrawalConfigTitle')}><p className="assets-metric__hint">{t('admin.walletDepositWithdrawalConfigDescription')}</p></Card> },
    { to: '/wallet/providers', label: t('admin.walletProvidersTab') },
    { to: '/wallet/withdrawal-policy', label: t('admin.walletWithdrawalPolicyTab') },
    { to: '/wallet/health-reconciliation', label: t('admin.walletHealthReconciliationTab') },
  ]} />;
}

export function AdminSpotWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.spot')} tabs={[{ to: '/spot/assets', label: t('admin.spotAssetConfigTitle'), content: <SpotAssetConfigurationPanel /> }]} />;
}

export function AdminFuturesWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.futures')} tabs={[{ to: '/futures/configuration', label: t('admin.nav.futures') }]} />;
}

export function AdminRiskWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.risk')} tabs={[{ to: '/risk/assets', label: t('admin.assetsRiskConfigTab'), content: <Card title={t('admin.assetsRiskConfigTitle')}><p className="assets-metric__hint">{t('admin.assetsRiskConfigDescription')}</p></Card> }]} />;
}

export function AdminMarketMakersWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.marketMakers')} tabs={[{ to: '/market-makers/configuration', label: t('admin.nav.marketMakers') }]} />;
}

export function AdminOperationLogsWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.operationLogs')} tabs={[{ to: '/operation-logs/list', label: t('admin.nav.operationLogs') }]} />;
}

export function AdminSettingsWorkspacePage() {
  const { t } = useI18n();
  return <AdminNavWorkspace title={t('admin.nav.settings')} tabs={[{ to: '/settings/general', label: t('admin.nav.settings') }]} />;
}
