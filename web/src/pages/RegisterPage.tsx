import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { PasswordField } from '../components/auth/PasswordField';
import { SliderCaptcha } from '../components/auth/SliderCaptcha';
import { translateAuthError } from '../features/auth/authText';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { hasValidPasswordLength } from '../features/auth/passwordPolicy';
import { useI18n } from '../i18n';

export function RegisterPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [registrationId, setRegistrationId] = useState<string | null>(null);
  const [numericCode, setNumericCode] = useState('');
  const [letterCode, setLetterCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, verifyRegistrationEmail } = useAuthentication();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    try {
      if (displayName.trim().length < 2) {
        throw new Error('Display name must be at least 2 characters.');
      }
      if (!hasValidPasswordLength(password)) {
        throw new Error('Password must be between 8 and 32 characters.');
      }
      if (password !== confirmPassword) {
        throw new Error('Passwords do not match.');
      }
      if (!acceptedTerms) {
        throw new Error('You must accept the terms.');
      }
      setCaptchaOpen(true);
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.register.errorGeneric'));
    }
  }

  /** CAPTCHA 通過後只發起 email 驗證，帳號與 session 仍不得在這一步建立。 */
  async function requestRegistrationVerification(captchaToken: string) {
    setLoading(true);
    setError(null);

    try {
      const pending = await register({ email: identifier, displayName, password, captchaToken });
      setRegistrationId(pending.registrationId);
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.register.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  /** 兩組 code 都只停留在受控 input state；成功後 server 才會建立帳號並回傳 HttpOnly session。 */
  async function completeEmailVerification(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (registrationId === null) return;
    setLoading(true);
    setError(null);
    try {
      await verifyRegistrationEmail({ registrationId, numericCode, letterCode });
      navigate('/');
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.register.errorVerificationInvalid'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      variant="register"
      formAreaRatio={5}
      footer={
        <p className="auth-page__helper">
          {t('auth.register.footerPrefix')} <NavLink to="/login">{t('auth.register.footerSignIn')}</NavLink>.
        </p>
      }
    >
      {registrationId === null ? <form className="auth-form" onSubmit={handleSubmit}>
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
      </form> : <form className="auth-form" onSubmit={completeEmailVerification}>
        <p className="auth-form__hint">{t('auth.register.emailVerificationHint', '驗證碼已寄至您的電子郵件；兩組皆須輸入正確。')}</p>
        <label className="field">
          <span className="field__label">{t('auth.register.numericCode', '數字驗證碼')}</span>
          <input
            className="input"
            value={numericCode}
            onChange={(event) => setNumericCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
            placeholder={t('auth.register.numericCodePlaceholder', '6 位數字')}
            inputMode="numeric"
            autoComplete="one-time-code"
            required
          />
        </label>
        <label className="field">
          <span className="field__label">{t('auth.register.letterCode', '英文字母驗證碼')}</span>
          <input
            className="input"
            value={letterCode}
            onChange={(event) => setLetterCode(event.target.value.toUpperCase().replace(/[^A-Z]/g, '').slice(0, 5))}
            placeholder={t('auth.register.letterCodePlaceholder', '5 位英文字母')}
            autoCapitalize="characters"
            autoComplete="off"
            required
          />
        </label>
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        <button className="primary-button" type="submit" disabled={loading || numericCode.length !== 6 || letterCode.length !== 5}>
          {loading ? t('auth.register.verifyingEmail', '驗證中…') : t('auth.register.verifyEmail', '驗證並建立帳號')}
        </button>
      </form>}
      <SliderCaptcha
        open={captchaOpen}
        purpose="REGISTRATION"
        onCancel={() => setCaptchaOpen(false)}
        onVerified={(captchaToken) => {
          setCaptchaOpen(false);
          void requestRegistrationVerification(captchaToken);
        }}
      />
    </AuthPageShell>
  );
}
