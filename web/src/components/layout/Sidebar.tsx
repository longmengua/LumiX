import { Link, useLocation } from 'react-router-dom';

function normalizePathname(value: string) {
  return value.replace(/\/+$/, '') || '/';
}

function isSidebarItemActive(pathname: string, to: string) {
  const current = normalizePathname(pathname);
  const target = normalizePathname(to);

  if (target === '/account') {
    return current === '/account';
  }

  return current === target;
}

type SidebarProps = {
  title: string;
  items: Array<{ to: string; label: string; end?: boolean }>;
};

export function Sidebar({ title, items }: SidebarProps) {
  const location = useLocation();

  return (
    <div className="sidebar">
      <p className="sidebar__title">{title}</p>
      <nav className="sidebar__nav">
        {items.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            aria-current={isSidebarItemActive(location.pathname, item.to) ? 'page' : undefined}
            className={`sidebar__link${isSidebarItemActive(location.pathname, item.to) ? ' sidebar__link--active' : ''}`}
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </div>
  );
}
