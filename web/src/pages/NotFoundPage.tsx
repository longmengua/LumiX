import { useNavigate } from 'react-router-dom';

import { AdminErrorPage, NotFoundIcon } from '../admin/components/AdminErrorPage';
import { useI18n } from '../i18n';

export function NotFoundPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const goBack = () => window.history.length > 1 ? navigate(-1) : navigate('/', { replace: true });
  return <AdminErrorPage code="404" title={t('admin.errors.404.title')} description={t('admin.errors.404.description')} icon={<NotFoundIcon />} secondaryAction={<button className="secondary-button" type="button" onClick={goBack}>{t('admin.errors.back')}</button>} primaryAction={<button className="primary-button" type="button" onClick={() => navigate('/')}>{t('common.backHome')}</button>} />;
}
