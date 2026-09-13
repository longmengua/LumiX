import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom';

import { PasswordField } from '../../components/auth/PasswordField';
import { Card } from '../../components/base/Card';
import { Logo } from '../../components/brand/Logo';
import { translateAuthError } from '../../features/auth/authText';
import { hasValidPasswordLength } from '../../features/auth/passwordPolicy';
import { useI18n } from '../../i18n';
import { resetAdminPassword } from '../api/adminAuthenticationApi';

/** 一次性後台 token 只能在專用路由提交，成功後回到後台登入而非建立 session。 */
export function AdminResetPasswordPage() {
  const { locale, setLocale, t } = useI18n();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const token = searchParams.get('token') ?? '';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (!token) throw new Error('ADMIN_AUTH_REQUEST_FAILED');
      if (!hasValidPasswordLength(newPassword)) throw new Error('Password must be between 8 and 32 characters.');
      if (newPassword !== confirmPassword) throw new Error('Passwords do not match.');
      await resetAdminPassword({ token, newPassword });
      navigate('/login', { replace: true });
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'admin.auth.reset.errorGeneric'));
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
            <p className="eyebrow">{t('admin.auth.reset.eyebrow')}</p>
            <h1>{t('admin.auth.reset.title')}</h1>
            <p className="lead">{t('admin.auth.reset.subtitle')}</p>
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
        </div>
        <Card title={t('admin.auth.reset.cardTitle')}>
          <form className="auth-form" onSubmit={handleSubmit}>
            <PasswordField label={t('admin.auth.reset.newPassword')} name="new-password" value={newPassword} onChange={setNewPassword} autoComplete="new-password" />
            <PasswordField label={t('admin.auth.reset.confirmPassword')} name="confirm-password" value={confirmPassword} onChange={setConfirmPassword} autoComplete="new-password" />
            {error ? <p className="form-message form-message--error">{error}</p> : null}
            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? t('admin.auth.reset.submitting') : t('admin.auth.reset.submit')}
            </button>
            <p className="auth-page__helper"><NavLink to="/login">{t('admin.auth.forgot.backToLogin')}</NavLink></p>
          </form>
        </Card>
      </div>
    </div>
  );
}
