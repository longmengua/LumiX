import { useState, type FormEvent } from 'react';
import { NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { translateAuthError } from '../features/auth/authText';
import { requestPasswordReset } from '../features/auth/authApi';
import { useI18n } from '../i18n';

export function ForgotPasswordPage() {
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await requestPasswordReset(identifier);
      // server 對存在與不存在的 email 都採相同回應，UI 也不得自行推論帳號狀態。
      setSuccess('若該信箱可重設密碼，系統已寄出後續指示。');
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.forgot.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      variant="forgot"
      footer={
        <p className="auth-page__helper">
          {t('auth.forgot.footerPrefix')} <NavLink to="/login">{t('auth.forgot.footerSignIn')}</NavLink>{' '}
          {t('auth.forgot.footerConnector')} <NavLink to="/register">{t('auth.forgot.footerCreateAccount')}</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">{t('auth.forgot.identifier')}</span>
          <input
            className="input"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder={t('auth.forgot.identifierPlaceholder')}
            autoComplete="username"
          />
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.forgot.submitting') : t('auth.forgot.submit')}
        </button>
      </form>
    </AuthPageShell>
  );
}
