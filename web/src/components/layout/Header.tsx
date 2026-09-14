import { NavLink } from 'react-router-dom';

import { Logo } from '../brand/Logo';
import { useAuthentication } from '../../features/auth/AuthenticationProvider';
import { useI18n } from '../../i18n';

const links = [
  ['/markets', 'nav.markets'],
  ['/spot/BTC-USDT', 'nav.spot'],
  ['/futures/BTC-USDT', 'nav.futures'],
  ['/margin/BTC-USDT', 'nav.margin'],
  ['/assets', 'nav.assets'],
  ['/orders', 'nav.orders'],
  ['/positions', 'nav.positions'],
] as const;

export function Header() {
  const { locale, setLocale, t } = useI18n();
  const { loading, user } = useAuthentication();

  return (
    <header className="topbar">
      {/* 主站 header 只提供導覽與語系切換，避免把交易或資產操作塞在全域導覽裡。 */}
      <NavLink className="topbar__brand topbar__brand--full" to="/" aria-label={t('nav.logo')}>
        <Logo size="md" title={t('nav.logo')} variant="full" />
      </NavLink>
      <NavLink className="topbar__brand topbar__brand--mark" to="/" aria-label={t('nav.logo')}>
        <Logo size="md" title={t('nav.logo')} variant="mark" />
      </NavLink>
      <nav className="topbar__nav" aria-label="Primary">
        {links.map(([to, labelKey]) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `topbar__link${isActive ? ' topbar__link--active' : ''}`}
          >
            {t(labelKey)}
          </NavLink>
        ))}
      </nav>
      <div className="topbar__actions">
        <label className="topbar__locale-switcher">
          <GlobeIcon />
          <span className="sr-only">{t('header.language')}</span>
          <span className="topbar__locale-value" aria-hidden="true">
            {locale === 'zh-TW' ? t('locale.zh-TW') : t('locale.en-US')}
          </span>
          {/* 透明原生 select 覆蓋整個控制區，圖示與語系文字都能直接開啟選單且保有鍵盤操作。 */}
          <select className="topbar__locale-select" value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)} aria-label={t('header.language')}>
            <option value="zh-TW">{t('locale.zh-TW')}</option>
            <option value="en-US">{t('locale.en-US')}</option>
          </select>
          <ChevronDownIcon />
        </label>
        {loading ? null : user ? (
          <NavLink className="topbar__button" to="/account">{user.displayName}</NavLink>
        ) : null}
      </div>
    </header>
  );
}

function GlobeIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="8.5" />
      <path d="M3.8 12h16.4M12 3.5c2.1 2.3 3.2 5.1 3.2 8.5S14.1 18.2 12 20.5C9.9 18.2 8.8 15.4 8.8 12S9.9 5.8 12 3.5Z" />
    </svg>
  );
}

function ChevronDownIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="m7.5 9.5 4.5 4.5 4.5-4.5" />
    </svg>
  );
}
