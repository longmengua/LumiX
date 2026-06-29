import { Card } from '../components/base/Card';
import { PageHeader } from '../components/layout/PageHeader';
import { useI18n } from '../i18n';

export function NotFoundPage() {
  const { t } = useI18n();
  return (
    <>
      <PageHeader title={t('notFound.title')} description={t('notFound.description')} />
      <Card title={t('notFound.pageTitle')}>
        <p>{t('notFound.pageDescription')}</p>
      </Card>
    </>
  );
}
