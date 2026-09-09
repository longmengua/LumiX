import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { PasswordField } from '../components/auth/PasswordField';
import { translateAuthError } from '../features/auth/authText';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { useI18n } from '../i18n';

export function RegisterPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register } = useAuthentication();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (displayName.trim().length < 2) {
        throw new Error('Display name must be at least 2 characters.');
      }
      if (password.length < 8 || password.length > 32) {
        throw new Error('Password must be between 8 and 32 characters.');
      }
      if (password !== confirmPassword) {
        throw new Error('Passwords do not match.');
      }
      if (!acceptedTerms) {
        throw new Error('You must accept the terms.');
      }
      await register({ email: identifier, displayName, password });
      navigate('/');
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.register.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      variant="register"
      footer={
        <p className="auth-page__helper">
          {t('auth.register.footerPrefix')} <NavLink to="/login">{t('auth.register.footerSignIn')}</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">{t('auth.register.identifier')}</span>
          <input
            className="input"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder={t('auth.register.identifierPlaceholder')}
            autoComplete="username"
          />
        </label>

        <label className="field">
          <span className="field__label">顯示名稱</span>
          <input
            className="input"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            placeholder="至少 2 個字元"
            autoComplete="name"
          />
        </label>

        <div className="auth-form__split">
          <PasswordField
            label={t('auth.register.password')}
            value={password}
            onChange={setPassword}
            placeholder={t('auth.register.passwordPlaceholder')}
            autoComplete="new-password"
          />
          <PasswordField
            label={t('auth.register.confirmPassword')}
            value={confirmPassword}
            onChange={setConfirmPassword}
            placeholder={t('auth.register.confirmPasswordPlaceholder')}
            autoComplete="new-password"
          />
        </div>

        <label className="checkbox">
          <input checked={acceptedTerms} type="checkbox" onChange={(event) => setAcceptedTerms(event.target.checked)} />
          <span>{t('auth.register.acceptTerms')}</span>
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.register.submitting') : t('auth.register.submit')}
        </button>
      </form>
    </AuthPageShell>
  );
}
