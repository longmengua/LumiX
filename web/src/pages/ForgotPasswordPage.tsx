import { useState, type FormEvent } from 'react';
import { NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { translateAuthError, translateMaskedMessage } from '../features/auth/authText';
import { requestPasswordResetMock } from '../features/auth/mockAuthService';
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
      const result = await requestPasswordResetMock(identifier);
      setSuccess(translateMaskedMessage(result, t, 'auth.forgot.success', 'auth.forgot.successFallback'));
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
