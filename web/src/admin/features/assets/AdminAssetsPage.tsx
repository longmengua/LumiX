import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { PageHeader } from '../../../components/layout/PageHeader';
import { useI18n } from '../../../i18n';
import { AssetAdjustmentPanel } from './AssetAdjustmentPanel';
import { AssetAnalysisPanel } from './AssetAnalysisPanel';
import { AssetUserSearchPanel } from './AssetUserSearchPanel';

type AssetSection = 'users' | 'analysis' | 'adjustments';

const SECTION_PATHS: Record<AssetSection, string> = {
  users: '/assets/users',
  adjustments: '/assets/adjustments',
  analysis: '/assets/analysis',
};

/**
 * 真實管理端資產工作台。
 *
 * 受控資產命令只送到 server-side super-admin boundary；browser 不計算餘額、不產生假成功，也不保存敏感資產狀態。
 */
export function AdminAssetsPage() {
  const { t } = useI18n();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const normalizedPath = pathname.replace(/\/+$/, '') || '/';
  const section = sectionFromPath(normalizedPath);

  // 舊的 /assets 入口導向可分享的預設子頁，避免重整後只依賴 component 的暫存 state。
  if (normalizedPath === '/assets') return <Navigate replace to={SECTION_PATHS.users} />;
  // 原對帳頁改由分析中的可分享 tab 承接，保留既有書籤與內部連結。
  if (normalizedPath.startsWith('/assets/audit')) return <Navigate replace to="/assets/analysis?tab=reconciliation" />;
  // 現貨幣種設定已歸屬現貨模組；保留轉址可讓既有書籤安全遷移。
  if (normalizedPath.startsWith('/assets/spot-assets')) return <Navigate replace to="/spot/assets" />;
  // 充提幣相關設定已歸屬錢包模組；既有書籤仍導向新的可分享頁籤網址。
  if (normalizedPath.startsWith('/assets/wallet')) return <Navigate replace to="/wallet/asset-networks" />;
  // 資產風控已歸屬風控模組，避免資產工作台與風控政策分散在不同入口。
  if (normalizedPath.startsWith('/assets/risk')) return <Navigate replace to="/risk/assets" />;
  // 補償已不再提供於此工作台；既有連結導回唯一預設的帳務異常沖銷頁。
  if (normalizedPath.endsWith('/compensation')) return <Navigate replace to={SECTION_PATHS.adjustments} />;
  // 類型是表單欄位，不再以路徑拆成多個頁面；舊書籤統一導回單一資產調整工作台。
  if (normalizedPath.startsWith('/assets/adjustments/')) return <Navigate replace to={SECTION_PATHS.adjustments} />;

  return (
    <div className="stack">
      <PageHeader title={t('admin.assetsRuntimeTitle')} description={t('admin.assetsRuntimeDescription')} />
      <div className="admin-assets-workspace">
        <aside className="admin-assets-workspace__sidebar" aria-label={t('admin.assetsSections')}>
          <button className={`admin-assets-workspace__tab${section === 'users' ? ' admin-assets-workspace__tab--active' : ''}`} type="button" onClick={() => navigate(SECTION_PATHS.users)}>{t('admin.assetsUsersTab')}</button>
          <button className={`admin-assets-workspace__tab${section === 'analysis' ? ' admin-assets-workspace__tab--active' : ''}`} type="button" onClick={() => navigate(SECTION_PATHS.analysis)}>{t('admin.assetsAnalysisTab')}</button>
          <button className={`admin-assets-workspace__tab${section === 'adjustments' ? ' admin-assets-workspace__tab--active' : ''}`} type="button" onClick={() => navigate(SECTION_PATHS.adjustments)}>{t('admin.assetsAdjustmentsTab')}</button>
        </aside>
        <div className="admin-assets-workspace__content">
          {section === 'users' ? <AssetUserSearchPanel /> : null}
          {section === 'analysis' ? <AssetAnalysisPanel /> : null}
          {section === 'adjustments' ? <AssetAdjustmentPanel /> : null}
        </div>
      </div>
    </div>
  );
}

function sectionFromPath(pathname: string): AssetSection {
  if (pathname.startsWith('/assets/analysis')) return 'analysis';
  if (pathname.startsWith('/assets/users')) return 'users';
  return 'adjustments';
}
