import { useEffect, useMemo, useState } from 'react';
import { Route, Routes } from 'react-router-dom';

import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { Sidebar } from '../../components/layout/Sidebar';
import { useI18n } from '../../i18n';
import { formatTime } from '../../utils/format';
import { useAdminAuth } from '../auth/AdminAuthProvider';
import { fetchAdminLoginHistory, type AdminLoginHistoryRecord } from '../api/adminAccountApi';

/**
 * 管理員帳戶頁只呈現 server 已允許的本人資訊與登入紀錄。
 *
 * 後台登入不使用前台裝置綁定契約，因此刻意不建立「已綁定裝置」tab；裝置標籤與 IP 直接顯示於登入紀錄。
 */
export function AdminAccountPage() {
  const { t } = useI18n();
  const { signOut } = useAdminAuth();
  const items = useMemo(() => [
    { to: '/account', label: t('admin.account.overview'), end: true },
    { to: '/account/login-history', label: t('admin.account.loginHistory'), end: true },
  ], [t]);

  return (
    <div className="two-column admin-account">
      <Sidebar
        title={t('admin.account.sidebarTitle')}
        items={items}
        footer={<button className="sidebar__action" type="button" onClick={() => void signOut()}>{t('header.signOut')}</button>}
      />
      <div className="stack">
        <Routes>
          <Route index element={<AdminAccountOverview />} />
          <Route path="login-history" element={<AdminLoginHistoryPage />} />
        </Routes>
      </div>
    </div>
  );
}

function AdminAccountOverview() {
  const { t } = useI18n();
  const { session } = useAdminAuth();
  return (
    <div className="stack">
      <PageHeader title={t('admin.account.title')} description={t('admin.account.description')} />
      <Card title={t('admin.account.profileTitle')}>
        <dl className="stat-list">
          <div className="stat-list__row"><dt>{t('admin.account.displayName')}</dt><dd>{session?.displayName ?? '—'}</dd></div>
          <div className="stat-list__row"><dt>{t('admin.account.email')}</dt><dd>{session?.email ?? '—'}</dd></div>
        </dl>
      </Card>
      <Card title={t('admin.account.loginHistory')}>
        <p className="account-row__meta">{t('admin.account.loginHistoryHint')}</p>
      </Card>
    </div>
  );
}

function AdminLoginHistoryPage() {
  const { t } = useI18n();
  const [records, setRecords] = useState<AdminLoginHistoryRecord[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    void fetchAdminLoginHistory()
      .then((value) => { if (active) setRecords(value); })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'ADMIN_LOGIN_HISTORY_REQUEST_FAILED'); });
    return () => { active = false; };
  }, []);

  if (records === null && error === null) return <LoadingState title={t('admin.account.loginHistory')} description={t('admin.account.loginHistoryLoading')} />;
  if (error && records === null) return <ErrorState title={t('admin.account.loginHistory')} description={error} />;

  return (
    <div className="stack">
      <PageHeader title={t('admin.account.loginHistory')} description={t('admin.account.loginHistoryHint')} />
      <Card title={t('admin.account.loginHistory')}>
        {records?.length === 0 ? <EmptyState title={t('admin.account.loginHistory')} description={t('admin.account.loginHistoryEmpty')} /> : null}
        <div className="timeline-list">
          {records?.map((record) => (
            <div className="timeline-item" key={record.occurredAt}>
              <div>
                <p className="account-row__title">{record.deviceLabel ?? t('admin.account.unknownDevice')}</p>
                <p className="account-row__meta">{t('admin.account.loginHistoryIp', undefined, { ipAddress: record.ipAddress ?? '—' })}</p>
              </div>
              <time className="timeline-item__time" dateTime={record.occurredAt}>{formatTime(record.occurredAt)}</time>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
