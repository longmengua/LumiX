import { NavLink } from 'react-router-dom';
import { useI18n } from '../../i18n';

const links = [
  ['/', 'nav.logo'],
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
      <nav className="topbar__nav" aria-label="Primary">
        {links.map(([to, labelKey]) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `topbar__link${isActive ? ' topbar__link--active' : ''}`}
            end={to === '/'}
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
