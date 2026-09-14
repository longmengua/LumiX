import { NavLink } from 'react-router-dom';

import { Logo } from '../../components/brand/Logo';
import { useI18n } from '../../i18n';
import { adminNavItems } from '../adminNav';
import { useAdminAuth } from '../auth/AdminAuthProvider';

/** 後台 header 沿用前台的導覽語言，但帳戶入口只會前往管理端自己的 `/admin/account`。 */
export function AdminHeader() {
  const { locale, setLocale, t } = useI18n();
  const { session } = useAdminAuth();

  return (
    <header className="topbar admin-header">
      <NavLink className="topbar__brand topbar__brand--full" to="/" aria-label={t('admin.header.brandAria')}>
        <Logo size="md" title={t('nav.logo')} variant="full" />
      </NavLink>
      <NavLink className="topbar__brand topbar__brand--mark" to="/" aria-label={t('admin.header.brandAria')}>
        <Logo size="md" title={t('nav.logo')} variant="mark" />
      </NavLink>
      <nav className="topbar__nav admin-header__nav" aria-label={t('admin.topNavLabel')}>
        {adminNavItems.map(({ to, labelKey }) => (
          <NavLink key={to} className={({ isActive }) => `topbar__link${isActive ? ' topbar__link--active' : ''}`} to={to} end={to === '/'}>
            {t(labelKey)}
          </NavLink>
        ))}
      </nav>
      <div className="topbar__actions">
        <label className="topbar__locale-switcher">
          <GlobeIcon />
          <span className="sr-only">{t('header.language')}</span>
          <span className="topbar__locale-value" aria-hidden="true">{locale === 'zh-TW' ? t('locale.zh-TW') : t('locale.en-US')}</span>
          <select className="topbar__locale-select" value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)} aria-label={t('header.language')}>
            <option value="zh-TW">{t('locale.zh-TW')}</option>
            <option value="en-US">{t('locale.en-US')}</option>
          </select>
          <ChevronDownIcon />
        </label>
        {session ? <NavLink className="topbar__button" to="/account">{session.displayName}</NavLink> : null}
      </div>
    </header>
  );
}

function GlobeIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5" /><path d="M3.8 12h16.4M12 3.5c2.1 2.3 3.2 5.1 3.2 8.5S14.1 18.2 12 20.5C9.9 18.2 8.8 15.4 8.8 12S9.9 5.8 12 3.5Z" /></svg>;
}

function ChevronDownIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m7.5 9.5 4.5 4.5 4.5-4.5" /></svg>;
}
