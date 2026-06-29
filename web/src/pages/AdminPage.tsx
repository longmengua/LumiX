import { Sidebar } from '../components/layout/Sidebar';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/base/Card';
import { useI18n } from '../i18n';
import { adminNavItems } from '../features/navigation/adminNav';

export function AdminPage() {
  const { t } = useI18n();
  return (
    <div className="two-column">
      <Sidebar title={t('admin.pageTitle')} items={adminNavItems.map(({ to, labelKey }) => ({ to, label: t(labelKey) }))} />
      <div className="stack">
        <PageHeader title={t('admin.pageTitle')} description={t('admin.pageDescription')} />
        <Card title={t('admin.dashboardTitle')}>
          <p>{t('admin.message')}</p>
        </Card>
      </div>
    </div>
  );
}
