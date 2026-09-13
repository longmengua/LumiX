import { useEffect, useState, type FormEvent } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { PasswordField } from '../components/auth/PasswordField';
import { SliderCaptcha } from '../components/auth/SliderCaptcha';
import { translateAuthError } from '../features/auth/authText';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { useI18n } from '../i18n';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [verificationPending, setVerificationPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { signIn, completeLoginVerification } = useAuthentication();
  // 只接受站內相對路徑，避免把登入成功後的導向交給不可信的 location state。
  const requestedReturnTo = location.state?.returnTo;
  const returnTo = typeof requestedReturnTo === 'string'
    && requestedReturnTo.startsWith('/')
    && !requestedReturnTo.startsWith('//')
    ? requestedReturnTo
    : '/';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setCaptchaOpen(true);
  }

  /** 驗證碼通過後才開始真正登入，確保 token 永遠由後端一次性消耗。 */
  async function completeSignIn(captchaToken: string) {
    setLoading(true);
    setError(null);

    try {
      const authenticatedUser = await signIn({ email: identifier, password, captchaToken });
      if (authenticatedUser) {
        navigate(returnTo, { replace: true });
      } else {
        setVerificationPending(true);
      }
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.login.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!verificationPending) return undefined;
    let alive = true;
    let inFlight = false;

    // 原始登入頁持有 HttpOnly pending cookie，核准後由它輪詢完成一次登入；確認信頁不會取得 session。
    const pollCompletion = () => {
      if (inFlight) return;
      inFlight = true;
      void completeLoginVerification()
        .then((authenticatedUser) => {
          if (alive && authenticatedUser) navigate(returnTo, { replace: true });
        })
        .catch(() => {
          // 尚未核准與拒絕都不可把畫面視為登入成功；使用者仍可重新載入並以新流程登入。
        })
        .finally(() => {
          inFlight = false;
        });
    };

    pollCompletion();
    const intervalId = window.setInterval(pollCompletion, 2_500);
    return () => {
      alive = false;
      window.clearInterval(intervalId);
    };
  }, [completeLoginVerification, navigate, returnTo, verificationPending]);

  return (
    <AuthPageShell
      variant="login"
      formAreaRatio={3}
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

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {verificationPending ? <p className="form-message form-message--success">{t('auth.login.verificationPending')}</p> : null}

        <button className="primary-button" type="submit" disabled={loading || verificationPending}>
          {loading ? t('auth.login.submitting') : t('auth.login.submit')}
        </button>

      </form>
      <SliderCaptcha
        open={captchaOpen}
        purpose="LOGIN"
        onCancel={() => setCaptchaOpen(false)}
        onVerified={(captchaToken) => {
          setCaptchaOpen(false);
          void completeSignIn(captchaToken);
        }}
      />
    </AuthPageShell>
  );
}
