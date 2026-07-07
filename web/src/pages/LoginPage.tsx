import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { translateAuthError, translateMaskedMessage } from '../features/auth/authText';
import { signInMock } from '../features/auth/mockAuthService';
import { useI18n } from '../i18n';

export function LoginPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // 登入結果可能會導向 2FA；這裡只處理前端導頁，不負責憑證核發。
      const result = await signInMock({ identifier, password, remember });

      if (result.status === 'two_factor') {
        const challenge = translateMaskedMessage(
          result.challengeLabel,
          t,
          'auth.twoFactor.challenge',
          'auth.twoFactor.challengeFallback',
        );
        navigate(`/two-factor?next=${encodeURIComponent(result.nextPath)}&challenge=${encodeURIComponent(challenge)}`);
        return;
      }

      navigate(result.nextPath);
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.login.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      variant="login"
      homeLabel={t('auth.login.backHome')}
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

        <label className="field">
          <span className="field__label">{t('auth.login.password')}</span>
          <input
            className="input"
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={t('auth.login.passwordPlaceholder')}
            autoComplete="current-password"
          />
        </label>

        <div className="auth-form__row">
          <label className="checkbox">
            <input checked={remember} type="checkbox" onChange={(event) => setRemember(event.target.checked)} />
            <span>{t('auth.login.rememberMe')}</span>
          </label>
          <NavLink className="auth-form__link" to="/forgot-password">
            {t('auth.login.forgotPassword')}
          </NavLink>
        </div>

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.login.submitting') : t('auth.login.submit')}
        </button>

        <p className="auth-form__hint">{t('auth.login.hint')}</p>
      </form>
    </AuthPageShell>
  );
}
