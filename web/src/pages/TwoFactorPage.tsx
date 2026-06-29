import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { translateAuthError } from '../features/auth/authText';
import { verifyTwoFactorMock } from '../features/auth/mockAuthService';
import { useI18n } from '../i18n';

export function TwoFactorPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { t } = useI18n();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const challenge = searchParams.get('challenge') ?? t('auth.twoFactor.challengeFallback');
  const nextPath = searchParams.get('next') ?? '/';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await verifyTwoFactorMock(code);
      navigate(nextPath);
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.twoFactor.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow={t('auth.shell.eyebrow')}
      title={t('auth.twoFactor.title')}
      description={challenge}
      footer={
        <p className="auth-page__helper">
          {t('auth.twoFactor.footerPrefix')} <NavLink to="/login">{t('auth.twoFactor.footerSignIn')}</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">{t('auth.twoFactor.code')}</span>
          <input
            className="input"
            inputMode="numeric"
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder={t('auth.twoFactor.codePlaceholder')}
          />
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.twoFactor.submitting') : t('auth.twoFactor.submit')}
        </button>

        <p className="auth-form__hint">{t('auth.twoFactor.hint')}</p>
      </form>
    </AuthPageShell>
  );
}
