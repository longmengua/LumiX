import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { translateAuthError, translateMaskedMessage } from '../features/auth/authText';
import { registerMock } from '../features/auth/mockAuthService';
import { useI18n } from '../i18n';

export function RegisterPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [referralCode, setReferralCode] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await registerMock({
        identifier,
        verificationCode,
        password,
        confirmPassword,
        referralCode: referralCode.trim() || undefined,
        acceptedTerms,
      });

      setSuccess(
        translateMaskedMessage(result, t, 'auth.register.success', 'auth.register.successFallback'),
      );
      navigate('/login');
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

        <div className="auth-form__split">
          <label className="field">
            <span className="field__label">{t('auth.register.verificationCode')}</span>
            <input
              className="input"
              value={verificationCode}
              onChange={(event) => setVerificationCode(event.target.value)}
              placeholder={t('auth.register.verificationCodePlaceholder')}
            />
          </label>
          <label className="field">
            <span className="field__label">{t('auth.register.referralCode')}</span>
            <input
              className="input"
              value={referralCode}
              onChange={(event) => setReferralCode(event.target.value)}
              placeholder={t('auth.register.referralCodePlaceholder')}
            />
          </label>
        </div>

        <div className="auth-form__split">
          <label className="field">
            <span className="field__label">{t('auth.register.password')}</span>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder={t('auth.register.passwordPlaceholder')}
              autoComplete="new-password"
            />
          </label>
          <label className="field">
            <span className="field__label">{t('auth.register.confirmPassword')}</span>
            <input
              className="input"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder={t('auth.register.confirmPasswordPlaceholder')}
              autoComplete="new-password"
            />
          </label>
        </div>

        <label className="checkbox">
          <input checked={acceptedTerms} type="checkbox" onChange={(event) => setAcceptedTerms(event.target.checked)} />
          <span>{t('auth.register.acceptTerms')}</span>
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.register.submitting') : t('auth.register.submit')}
        </button>
      </form>
    </AuthPageShell>
  );
}
