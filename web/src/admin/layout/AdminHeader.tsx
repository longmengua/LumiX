import { NavLink, useNavigate } from 'react-router-dom';

import { Badge } from '../../components/base/Badge';
import { Logo } from '../../components/brand/Logo';
import { useI18n } from '../../i18n';
import { useAdminAuth } from '../auth/AdminAuthProvider';

export function AdminHeader() {
  const { locale, setLocale, t } = useI18n();
  const { session, signOut } = useAdminAuth();
  const navigate = useNavigate();

  return (
    <header className="admin-header">
      <NavLink className="admin-header__brand" to="/" aria-label={t('admin.header.brandAria')}>
        <Logo size="md" title={t('nav.logo')} variant="mark" />
        <div className="admin-header__brand-copy">
          <strong>{t('admin.header.brandTitle')}</strong>
          <span>{t('admin.header.brandSubtitle')}</span>
        </div>
      </NavLink>

      <div className="admin-header__actions">
        <Badge tone="warning">{t('admin.environment.development')}</Badge>
        <Badge tone="neutral">{t('admin.auth.operator', undefined, { operator: session?.email ?? t('admin.header.operatorFallback') })}</Badge>
        <label className="admin-header__locale-switcher">
          <span className="admin-header__locale-label">{t('header.language')}</span>
          <select className="admin-header__locale-select" value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)}>
            <option value="zh-TW">{t('locale.zh-TW')}</option>
            <option value="en-US">{t('locale.en-US')}</option>
          </select>
        </label>
        <button
          className="secondary-button"
          type="button"
          onClick={() => {
            signOut();
            navigate('/login', { replace: true });
          }}
        >
          {t('admin.header.signOut')}
        </button>
      </div>
    </header>
  );
}
