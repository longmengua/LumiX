import { NavLink } from 'react-router-dom';

import { useI18n } from '../../i18n';
import { adminNavItems } from '../adminNav';

export function AdminTopNav() {
  const { t } = useI18n();

  return (
    <nav className="admin-top-nav" aria-label={t('admin.topNavLabel')}>
      {/* 後台導覽只負責切換管理區塊，避免把操作意圖藏在頁面內部按鈕裡。 */}
      <div className="admin-top-nav__list">
        {adminNavItems.map(({ to, labelKey }) => (
          <NavLink key={to} className={({ isActive }) => `admin-top-nav__link${isActive ? ' admin-top-nav__link--active' : ''}`} to={to}>
            {t(labelKey)}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
