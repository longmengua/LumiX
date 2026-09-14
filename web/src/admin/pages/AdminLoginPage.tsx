import { useEffect, useState, type FormEvent } from 'react';
import { Navigate, NavLink, useNavigate } from 'react-router-dom';

import { Card } from '../../components/base/Card';
import { Logo } from '../../components/brand/Logo';
import { PasswordField } from '../../components/auth/PasswordField';
import { SliderCaptcha } from '../../components/auth/SliderCaptcha';
import { translateAuthError } from '../../features/auth/authText';
import { useI18n } from '../../i18n';
import { signInAdmin } from '../api/adminAuthenticationApi';
import { fetchAdminSession } from '../api/adminSessionApi';
import { useAdminAuth } from '../auth/AdminAuthProvider';

export function AdminLoginPage() {
  // 後台必須維持自己的登入入口；雖共用受控的 HttpOnly session 契約，權限仍由後端 admin principal 決定。
  const { locale, setLocale, t } = useI18n();
  const { isAuthenticated, loading } = useAdminAuth();
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const statusMessage = error;
  const statusTone = error ? 'form-message--error' : '';

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (!loading && isAuthenticated) {
    return <Navigate replace to="/" />;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setCaptchaOpen(true);
  }

  /**
   * 管理端只接受專用 endpoint 發出的 admin session，避免一般客戶 session 被誤導向後台首頁。
   * 失敗訊息刻意不區分帳密、帳號狀態或權限不足，避免成為帳號與權限枚舉來源。
   */
  async function completeAdminSignIn(captchaToken: string) {
    setSubmitting(true);
    setError(null);

    try {
      await signInAdmin({ email: identifier, password, captchaToken });
      await enterAdminConsole();
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'admin.auth.login.errorGeneric'));
    } finally {
      setSubmitting(false);
    }
  }

  async function enterAdminConsole() {
    await fetchAdminSession();
    // 重新掛載後台 provider，讓畫面只使用 server 查驗過的管理員 session 投影。
    window.location.assign('/admin');
  }

  return (
    <div className="admin-login admin-login--portal">
      <div className="admin-login__ambient-ellipse" aria-hidden="true" />
      {/* 語言切換器放在正常 header 流程，矮視窗捲動時仍會和其他內容一起可達。 */}
      <header className="admin-login__portal-header">
        <label className="admin-login__language-switcher">
          <GlobeIcon />
          <span className="sr-only">{t('header.language')}</span>
          <span className="admin-login__language-label" aria-hidden="true">
            {locale === 'zh-TW' ? t('locale.zh-TW') : t('locale.en-US')}
          </span>
          {/* 透明原生 select 覆蓋整個控制區，讓圖示、文字與箭頭都有相同且可鍵盤操作的 hit area。 */}
          <select value={locale} onChange={(event) => setLocale(event.target.value as typeof locale)} aria-label={t('header.language')}>
            <option value="zh-TW">{t('locale.zh-TW')}</option>
            <option value="en-US">{t('locale.en-US')}</option>
          </select>
          <ChevronDownIcon />
        </label>
      </header>

      <main className="admin-login__portal-main">
        <Card className="admin-login__card">
          <div className="admin-login__identity">
            <Logo size="lg" title={t('nav.logo')} variant="full" />
            <div className="admin-login__heading">
              <h1>{t('admin.auth.login.portalTitle')}</h1>
              <p>{t('admin.auth.login.portalSubtitle')}</p>
            </div>
          </div>

          <form className="admin-login__form auth-form--credential" onSubmit={handleSubmit}>

            <label className="admin-login__field">
              <span>{t('admin.auth.login.email')}</span>
              <span className="admin-login__input-shell">
                <MailIcon />
                <input
                className="input admin-login__input auth-input--with-leading-icon"
                name="identifier"
                value={identifier}
                onChange={(event) => setIdentifier(event.target.value)}
                placeholder={t('admin.auth.login.emailPlaceholder')}
                autoComplete="username"
                />
              </span>
            </label>

            <PasswordField
              label={t('admin.auth.login.password')}
              name="password"
              value={password}
              onChange={setPassword}
              placeholder={t('admin.auth.login.passwordPlaceholder')}
              autoComplete="current-password"
              className="admin-login__field"
              inputClassName="admin-login__input"
              leadingAdornment={<LockIcon />}
              iconOnlyToggle
              showPasswordLabel={t('admin.auth.login.showPassword')}
              hidePasswordLabel={t('admin.auth.login.hidePassword')}
              capsLockWarning={t('admin.auth.login.capsLockOn')}
            />
            <p className="admin-login__forgot-password">
              <NavLink to="/forgot-password">{t('admin.auth.login.forgotPassword')}</NavLink>
            </p>

            {/* 狀態區固定保留一行高度，避免錯誤訊息出現時推動下方按鈕與連結。 */}
            <p className={`form-message admin-login__status ${statusTone}`} aria-live="polite">
              {statusMessage ?? '\u00a0'}
            </p>

            <button className="primary-button admin-login__submit" type="submit" disabled={submitting}>
              {submitting ? t('admin.auth.login.submitting') : t('admin.auth.login.submit')}
              <ArrowRightIcon />
            </button>
          </form>
          <p className="admin-login__security-notice"><ShieldIcon />{t('admin.auth.login.securityNotice')}</p>
        </Card>
      </main>
      <footer className="admin-login__footer">
        <p>{t('admin.auth.login.footerTagline')}</p>
        <p>{t('admin.auth.login.contactAdmin')}</p>
      </footer>
      <SliderCaptcha
        open={captchaOpen}
        purpose="LOGIN"
        authBasePath="/api/admin/v1/auth"
        onCancel={() => setCaptchaOpen(false)}
        onVerified={(captchaToken) => {
          setCaptchaOpen(false);
          void completeAdminSignIn(captchaToken);
        }}
      />
    </div>
  );
}

function MailIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="5.5" width="17" height="13" rx="2" /><path d="m4.5 7 7.5 5.6L19.5 7" /></svg>;
}

function LockIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10.5" width="14" height="10" rx="2" /><path d="M8 10.5V7.8a4 4 0 0 1 8 0v2.7" /><path d="M12 14.5v2.2" /></svg>;
}

function GlobeIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="M3.8 12h16.4M12 3.5c2.1 2.3 3.2 5.1 3.2 8.5S14.1 18.2 12 20.5C9.9 18.2 8.8 15.4 8.8 12S9.9 5.8 12 3.5Z" /></svg>;
}

function ChevronDownIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7.5 9.5 4.5 4.5 4.5-4.5" /></svg>;
}

function ArrowRightIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h13M13.5 6.5 19 12l-5.5 5.5" /></svg>;
}

function ShieldIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3.5 19 6v5.4c0 4.3-2.8 7.3-7 9.1-4.2-1.8-7-4.8-7-9.1V6l7-2.5Z" /><path d="m8.9 12.1 2 2 4.2-4.3" /></svg>;
}
