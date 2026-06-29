import { NavLink } from 'react-router-dom';

const links = [
  { to: '/assets', label: 'Overview' },
  { to: '/assets/spot', label: 'Spot' },
  { to: '/assets/futures', label: 'Futures' },
  { to: '/assets/margin', label: 'Margin' },
  { to: '/assets/transfer', label: 'Transfer' },
  { to: '/assets/deposit', label: 'Deposit' },
  { to: '/assets/withdraw', label: 'Withdraw' },
  { to: '/assets/deposit/history', label: 'Deposit History' },
  { to: '/assets/withdraw/history', label: 'Withdraw History' },
  { to: '/assets/withdraw/addresses', label: 'Withdraw Addresses' },
] as const;

export function WalletSectionNav() {
  return (
    <section className="card">
      <h2 className="card__title">Asset Workspaces</h2>
      <p className="assets-tabs__hint">Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.</p>
      <div className="wallet-section-nav">
        {links.map((item) => (
          <NavLink key={item.to} className={({ isActive }) => `tab-button${isActive ? ' tab-button--active' : ''}`} to={item.to} end={item.to === '/assets'}>
            {item.label}
          </NavLink>
        ))}
      </div>
    </section>
  );
}
