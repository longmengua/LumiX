import { useNavigate } from 'react-router-dom';

import { AdminErrorPage, NotFoundIcon, ServerErrorIcon, ShieldErrorIcon } from '../components/AdminErrorPage';
import { useI18n } from '../../i18n';

type Status = 'forbidden' | 'not-found' | 'gone' | 'server';

/** 將 router、權限 guard 與 boundary 的狀態轉為同一套 ErrorPage，並保留 Admin Layout。 */
export function AdminStatusPage({ status }: { status: Status }) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const content = status === 'forbidden'
    ? { code: '403' as const, title: t('admin.errors.403.title'), description: t('admin.errors.403.description'), icon: <ShieldErrorIcon /> }
    : status === 'gone'
      ? { code: '410' as const, title: t('admin.errors.410.title'), description: t('admin.errors.410.description'), icon: <NotFoundIcon /> }
      : status === 'server'
        ? { code: '500' as const, title: t('admin.errors.500.title'), description: t('admin.errors.500.description'), icon: <ServerErrorIcon /> }
        : { code: '404' as const, title: t('admin.errors.404.title'), description: t('admin.errors.404.description'), icon: <NotFoundIcon /> };

  function goBack() {
    // 直接輸入錯誤網址時沒有可信的 app history，改回儀表板避免把使用者帶離管理端。
    if (window.history.length > 1) navigate(-1);
    else navigate('/', { replace: true });
  }

  const primaryAction = status === 'server'
    ? <button className="primary-button" type="button" onClick={() => window.location.reload()}>{t('admin.errors.retry')}</button>
    : <button className="primary-button" type="button" onClick={() => navigate('/')}>{t('admin.errors.dashboard')}</button>;

  return <AdminErrorPage {...content} secondaryAction={<button className="secondary-button" type="button" onClick={goBack}>{t('admin.errors.back')}</button>} primaryAction={primaryAction} />;
}
