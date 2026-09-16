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
          <NavLink key={to} className={({ isActive }) => `topbar__link admin-header__nav-link${isActive ? ' topbar__link--active' : ''}`} to={to} end={to === '/'}>
            <AdminNavIcon destination={to} />
            <span>{t(labelKey)}</span>
          </NavLink>
        ))}
      </nav>
      <div className="topbar__actions admin-header__actions">
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
        {session ? (
          <NavLink className="topbar__button admin-header__account" to="/account">
            <AdminBadgeIcon />
            <span>{session.displayName}</span>
          </NavLink>
        ) : null}
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

/** 帳戶入口採管理端識別 icon，僅輔助辨識，實際權限仍完全由既有 session 與路由守衛決定。 */
function AdminBadgeIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m5 9 2.2 2.2L10.5 6l3 5.2L17 9l1.8 5.5H6.2L5 9Z" /><path d="M7 18h10M9 21h6" /></svg>;
}

/** 導覽 icon 僅作為快速掃讀輔助，文字標籤仍保留，避免圖示成為唯一可理解的導覽資訊。 */
function AdminNavIcon({ destination }: { destination: string }) {
  let path;
  switch (destination) {
    case '/':
      path = <><rect x="4" y="4" width="6" height="6" rx="1" /><rect x="14" y="4" width="6" height="6" rx="1" /><rect x="4" y="14" width="6" height="6" rx="1" /><rect x="14" y="14" width="6" height="6" rx="1" /></>;
      break;
    case '/users':
      path = <><circle cx="9" cy="8" r="3" /><path d="M3.5 19a5.5 5.5 0 0 1 11 0M17 10.5a2.5 2.5 0 1 0 0-5M18.5 19a4.6 4.6 0 0 0-2.3-4" /></>;
      break;
    case '/assets':
      path = <><path d="m12 3 7.5 4.3v9.4L12 21l-7.5-4.3V7.3L12 3Z" /><path d="m4.5 7.3 7.5 4.3 7.5-4.3M12 11.6V21" /></>;
      break;
    case '/activities':
      path = <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M8 3v4M16 3v4M4 10h16M9 14h6M9 17h3" /></>;
      break;
    case '/wallet':
      path = <><path d="M4 7.5A2.5 2.5 0 0 1 6.5 5H18a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6.5A2.5 2.5 0 0 1 4 17.5v-10Z" /><path d="M4 8h13v4H4M16 14h4" /></>;
      break;
    case '/spot':
      path = <><path d="M5 18V9M12 18V5M19 18v-7" /><path d="M3 18h18" /></>;
      break;
    case '/futures':
      path = <><rect x="5" y="4" width="14" height="16" rx="2" /><path d="M9 8h6M9 12h6M9 16h3" /></>;
      break;
    case '/risk':
      path = <path d="M12 3 20 6v5.4c0 4.4-3.1 7.4-8 9.6-4.9-2.2-8-5.2-8-9.6V6l8-3Z" />;
      break;
    case '/market-makers':
      path = <><path d="M4 19h16M6 17V9l6-4 6 4v8M9 12h.01M15 12h.01" /><path d="M10 19v-3h4v3" /></>;
      break;
    case '/insurance-fund':
      path = <><path d="M12 3 19 6v5c0 4.5-2.8 7.6-7 10-4.2-2.4-7-5.5-7-10V6l7-3Z" /><path d="m8.5 12 2.2 2.2 4.8-4.8" /></>;
      break;
    case '/reconciliation':
      path = <><path d="M6 5h12v14H6zM9 9h6M9 13h6M9 17h3" /><path d="m17 16 1.3 1.3L21 14.6" /></>;
      break;
    case '/operation-logs':
      path = <><rect x="5" y="4" width="14" height="16" rx="2" /><path d="M9 8h6M9 12h6M9 16h4" /></>;
      break;
    case '/settings':
      path = <><circle cx="12" cy="12" r="3" /><path d="M19 12a7 7 0 0 0-.1-1.2l2-1.5-2-3.5-2.4 1A7.2 7.2 0 0 0 14.5 5L14 2.5h-4L9.5 5a7.2 7.2 0 0 0-2 1.1l-2.4-1-2 3.5 2 1.5A7 7 0 0 0 5 12c0 .4 0 .8.1 1.2l-2 1.5 2 3.5 2.4-1a7.2 7.2 0 0 0 2 1.1l.5 2.5h4l.5-2.5a7.2 7.2 0 0 0 2-1.1l2.4 1 2-3.5-2-1.5c.1-.4.1-.8.1-1.2Z" /></>;
      break;
    default:
      path = <circle cx="12" cy="12" r="7" />;
  }
  return <svg className="admin-header__nav-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">{path}</svg>;
}
