import { useEffect, useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';

import { Badge } from '../../components/base/Badge';
import { Card } from '../../components/base/Card';
import { Logo } from '../../components/brand/Logo';
import { useI18n } from '../../i18n';
import { useAdminAuth } from '../auth/AdminAuthProvider';

const SHOW_DEV_NOTICES = import.meta.env.VITE_SHOW_DEV_NOTICES === 'true';

export function AdminLoginPage() {
  // 這個頁面只處理後台 demo session 的登入體驗，不代表正式 operator 驗證流程。
  const { locale, setLocale, t } = useI18n();
  const { isAuthenticated, signIn } = useAdminAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('operator@lumix.exchange');
  const [password, setPassword] = useState('admin-demo');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (isAuthenticated) {
    return <Navigate replace to="/" />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await signIn({ email, password });
      navigate('/', { replace: true });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : t('admin.auth.login.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-login">
      <div className="admin-login__shell">
        <div className="admin-login__brand">
          <Logo size="lg" title={t('nav.logo')} variant="full" />
          <div>
            <p className="eyebrow">{t('admin.auth.login.eyebrow')}</p>
            <h1>{t('admin.auth.login.title')}</h1>
            <p className="lead">{t('admin.auth.login.subtitle')}</p>
          </div>
        </div>

        <div className="admin-login__toolbar">
          <label className="admin-header__locale-switcher">
            <span className="admin-header__locale-label">{t('header.language')}</span>
            <select className="admin-header__locale-select" value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)}>
              <option value="zh-TW">{t('locale.zh-TW')}</option>
              <option value="en-US">{t('locale.en-US')}</option>
            </select>
          </label>
          {SHOW_DEV_NOTICES ? <Badge tone="warning">{t('admin.notice.developmentAdapter')}</Badge> : null}
        </div>

        <Card title={t('admin.auth.login.cardTitle')}>
          <form className="auth-form" onSubmit={handleSubmit}>
            <label className="field">
              <span className="field__label">{t('admin.auth.login.email')}</span>
              <input className="input" value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>

            <label className="field">
              <span className="field__label">{t('admin.auth.login.password')}</span>
              <input className="input" type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
            </label>

            {error ? <p className="form-message form-message--error">{error}</p> : null}

            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? t('admin.auth.login.submitting') : t('admin.auth.login.submit')}
            </button>
          </form>
        </Card>

      </div>
    </div>
  );
}
