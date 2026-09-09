import { useState, type FormEvent } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { PasswordField } from '../components/auth/PasswordField';
import { translateAuthError } from '../features/auth/authText';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { useI18n } from '../i18n';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { signIn } = useAuthentication();
  // 只接受站內相對路徑，避免把登入成功後的導向交給不可信的 location state。
  const requestedReturnTo = location.state?.returnTo;
  const returnTo = typeof requestedReturnTo === 'string'
    && requestedReturnTo.startsWith('/')
    && !requestedReturnTo.startsWith('//')
    ? requestedReturnTo
    : '/';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await signIn({ email: identifier, password });
      navigate(returnTo, { replace: true });
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.login.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      variant="login"
      footer={
        <p className="auth-page__helper">
          {t('auth.login.newHere')}{' '}
          <NavLink to="/register">{t('auth.login.createAccount')}</NavLink>{' '}
          {t('auth.login.footerConnector')}{' '}
          <NavLink to="/forgot-password">{t('auth.login.forgotPassword')}</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">{t('auth.login.identifier')}</span>
          <input
            className="input"
            name="identifier"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder={t('auth.login.identifierPlaceholder')}
            autoComplete="username"
          />
        </label>

        <PasswordField
          label={t('auth.login.password')}
          name="password"
          value={password}
          onChange={setPassword}
          placeholder={t('auth.login.passwordPlaceholder')}
          autoComplete="current-password"
        />

        <div className="auth-form__row">
          <NavLink className="auth-form__link" to="/forgot-password">
            {t('auth.login.forgotPassword')}
          </NavLink>
        </div>

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.login.submitting') : t('auth.login.submit')}
        </button>

        <p className="auth-form__hint">登入狀態由同源 HttpOnly Cookie 保護，瀏覽器端不保存 session token。</p>
      </form>
    </AuthPageShell>
  );
}
