import { useEffect } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';

import { Badge } from '../../components/base/Badge';
import { Card } from '../../components/base/Card';
import { Logo } from '../../components/brand/Logo';
import { useI18n } from '../../i18n';
import { useAdminAuth } from '../auth/AdminAuthProvider';

const SHOW_DEV_NOTICES = import.meta.env.VITE_SHOW_DEV_NOTICES === 'true';

export function AdminLoginPage() {
  // 後台不再有獨立 mock 登入；最高管理員必須先完成一般登入，再由 server 檢查其 admin principal。
  const { locale, setLocale, t } = useI18n();
  const { isAuthenticated, loading } = useAdminAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (!loading && isAuthenticated) {
    return <Navigate replace to="/" />;
  }

  return (
    <div className="admin-login">
      <div className="admin-login__shell">
        <div className="admin-login__brand">
          <Logo size="lg" title={t('nav.logo')} variant="full" />
          <div>
            <p className="eyebrow">{t('admin.auth.login.eyebrow')}</p>
            <h1>{t('admin.auth.login.title')}</h1>
            <p className="lead">{t('admin.auth.login.subtitle')}</p>
          </div>
        </div>

        <div className="admin-login__toolbar">
          <label className="admin-header__locale-switcher">
            <span className="admin-header__locale-label">{t('header.language')}</span>
            <select className="admin-header__locale-select" value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)}>
              <option value="zh-TW">{t('locale.zh-TW')}</option>
              <option value="en-US">{t('locale.en-US')}</option>
            </select>
          </label>
          {SHOW_DEV_NOTICES ? <Badge tone="warning">{t('admin.notice.developmentAdapter')}</Badge> : null}
        </div>

        <Card title={t('admin.auth.login.cardTitle')}>
          <div className="auth-form">
            <p className="lead">請先以最高管理員帳號完成前台登入；後台會再由伺服器確認已啟用的管理員身分。</p>
            <a className="primary-button" href="/login?returnTo=%2Fadmin">前往登入</a>
          </div>
        </Card>

      </div>
    </div>
  );
}
