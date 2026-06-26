import { NavLink } from 'react-router-dom';

type SidebarProps = {
  title: string;
  items: Array<{ to: string; label: string }>;
};

export function Sidebar({ title, items }: SidebarProps) {
  return (
    <div className="sidebar">
      <p className="sidebar__title">{title}</p>
      <nav className="sidebar__nav">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar__link${isActive ? ' sidebar__link--active' : ''}`}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

