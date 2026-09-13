import { useState, type FormEvent } from 'react';
import { NavLink } from 'react-router-dom';

import { SliderCaptcha } from '../../components/auth/SliderCaptcha';
import { Card } from '../../components/base/Card';
import { Logo } from '../../components/brand/Logo';
import { translateAuthError } from '../../features/auth/authText';
import { useI18n } from '../../i18n';
import { requestAdminPasswordReset } from '../api/adminAuthenticationApi';

/** 後台專用復原頁只處理既有管理員帳號，刻意不提供註冊或一般客戶端導覽。 */
export function AdminForgotPasswordPage() {
  const { locale, setLocale, t } = useI18n();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setCaptchaOpen(true);
  }

  /** server 對不存在、非管理員與可復原帳戶均回傳相同結果，前端不可推論帳戶權限。 */
  async function completeRecovery(captchaToken: string) {
    setLoading(true);
    setError(null);
    try {
      await requestAdminPasswordReset({ email, captchaToken });
      setSuccess(t('admin.auth.forgot.success'));
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'admin.auth.forgot.errorGeneric'));
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
            <p className="eyebrow">{t('admin.auth.forgot.eyebrow')}</p>
            <h1>{t('admin.auth.forgot.title')}</h1>
            <p className="lead">{t('admin.auth.forgot.subtitle')}</p>
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
        <Card title={t('admin.auth.forgot.cardTitle')}>
          <form className="auth-form" onSubmit={handleSubmit}>
            <label className="field">
              <span className="field__label">{t('admin.auth.login.email')}</span>
              <input className="input" name="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" />
            </label>
            {error ? <p className="form-message form-message--error">{error}</p> : null}
            {success ? <p className="form-message form-message--success">{success}</p> : null}
            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? t('admin.auth.forgot.submitting') : t('admin.auth.forgot.submit')}
            </button>
            <p className="auth-page__helper"><NavLink to="/login">{t('admin.auth.forgot.backToLogin')}</NavLink></p>
          </form>
        </Card>
      </div>
      <SliderCaptcha
        open={captchaOpen}
        purpose="PASSWORD_RESET"
        onCancel={() => setCaptchaOpen(false)}
        onVerified={(captchaToken) => {
          setCaptchaOpen(false);
          void completeRecovery(captchaToken);
        }}
      />
    </div>
  );
}
