import { NavLink } from 'react-router-dom';

const links = [
  ['/', 'Logo'],
  ['/markets', 'Markets'],
  ['/spot/BTC-USDT', 'Spot'],
  ['/futures/BTC-USDT', 'Futures'],
  ['/margin/BTC-USDT', 'Margin'],
  ['/assets', 'Assets'],
  ['/orders', 'Orders'],
  ['/account', 'Account'],
] as const;

export function Header() {
  return (
    <header className="topbar">
      <nav className="topbar__nav" aria-label="Primary">
        {links.map(([to, label]) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `topbar__link${isActive ? ' topbar__link--active' : ''}`}
            end={to === '/'}
          >
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="topbar__actions">
        <NavLink className="topbar__button" to="/account">
          Login
        </NavLink>
      </div>
    </header>
  );
}

