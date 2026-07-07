import { NavLink } from 'react-router-dom';

import { Logo } from '../brand/Logo';
import { useI18n } from '../../i18n';

const links = [
  ['/markets', 'nav.markets'],
  ['/spot/BTC-USDT', 'nav.spot'],
  ['/futures/BTC-USDT', 'nav.futures'],
  ['/margin/BTC-USDT', 'nav.margin'],
  ['/assets', 'nav.assets'],
  ['/orders', 'nav.orders'],
  ['/positions', 'nav.positions'],
  ['/account', 'nav.account'],
] as const;

export function Header() {
  const { locale, setLocale, t } = useI18n();

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
          <span className="topbar__locale-label">{t('header.language')}</span>
          <select className="topbar__locale-select" value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)}>
            <option value="zh-TW">{t('locale.zh-TW')}</option>
            <option value="en-US">{t('locale.en-US')}</option>
          </select>
        </label>
        <NavLink className="topbar__button" to="/login">
          {t('header.signIn')}
        </NavLink>
      </div>
    </header>
  );
}
