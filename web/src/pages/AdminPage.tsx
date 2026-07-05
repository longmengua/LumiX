import { useEffect } from 'react';
import { NavLink } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { useI18n } from '../i18n';

export function AdminPage() {
  const { t } = useI18n();

  useEffect(() => {
    window.location.replace('/admin');
  }, []);

  return (
    <div className="stack">
      <Card title={t('admin.redirectTitle')}>
        <p>{t('admin.redirectDescription')}</p>
        <NavLink className="secondary-button" to="/admin">
          {t('admin.redirectLink')}
        </NavLink>
      </Card>
    </div>
  );
}
