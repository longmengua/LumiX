import { Card } from '../../components/base/Card';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';

/** 活動入口先維持沒有假資料的狀態，待活動設定 API 建立後才提供受治理的新增與啟用操作。 */
export function AdminActivitiesPage() {
  const { t } = useI18n();
  return <div className="admin-console stack">
    <PageHeader title={t('admin.activitiesTitle')} description={t('admin.activitiesDescription')} />
    <Card title={t('admin.activitiesUnavailableTitle')}><p className="assets-metric__hint">{t('admin.activitiesUnavailableDescription')}</p></Card>
  </div>;
}
