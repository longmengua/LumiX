import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom';

import { PasswordField } from '../../components/auth/PasswordField';
import { Card } from '../../components/base/Card';
import { Logo } from '../../components/brand/Logo';
import { translateAuthError } from '../../features/auth/authText';
import { hasValidNewPassword, MAX_PASSWORD_LENGTH, sanitizeNewPasswordInput } from '../../features/auth/passwordPolicy';
import { useI18n } from '../../i18n';
import { resetAdminPassword } from '../api/adminAuthenticationApi';

/** 一次性後台 token 只能在專用路由提交，成功後回到後台登入而非建立 session。 */
export function AdminResetPasswordPage() {
  const { locale, setLocale, t } = useI18n();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const token = searchParams.get('token') ?? '';
  const statusTone = error ? 'form-message--error' : '';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (!token) throw new Error('ADMIN_AUTH_REQUEST_FAILED');
      if (!hasValidNewPassword(newPassword)) throw new Error('Password must be between 8 and 32 characters.');
      if (newPassword !== confirmPassword) throw new Error('Passwords do not match.');
      await resetAdminPassword({ token, newPassword });
      navigate('/login', { replace: true });
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'admin.auth.reset.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-login admin-login--portal">
      <div className="admin-login__ambient-ellipse" aria-hidden="true" />
      <header className="admin-login__portal-header">
        <label className="admin-login__language-switcher">
          <GlobeIcon />
          <span className="sr-only">{t('header.language')}</span>
          <span className="admin-login__language-label" aria-hidden="true">
            {locale === 'zh-TW' ? t('locale.zh-TW') : t('locale.en-US')}
          </span>
          {/* 透明原生 select 保留瀏覽器與鍵盤可及性，外層沿用管理端登入入口的控制視覺。 */}
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
              <h1>{t('admin.auth.reset.title')}</h1>
              <p>{t('admin.auth.reset.subtitle')}</p>
            </div>
          </div>

          <form className="admin-login__form auth-form--credential" onSubmit={handleSubmit}>
            <PasswordField
              label={t('admin.auth.reset.newPassword')}
              name="new-password"
              value={newPassword}
              onChange={setNewPassword}
              autoComplete="new-password"
              maxLength={MAX_PASSWORD_LENGTH}
              sanitizeInput={sanitizeNewPasswordInput}
              passwordRuleHint={t('auth.password.rules')}
              className="admin-login__field"
              inputClassName="admin-login__input"
              leadingAdornment={<LockIcon />}
              iconOnlyToggle
            />
            <PasswordField
              label={t('admin.auth.reset.confirmPassword')}
              name="confirm-password"
              value={confirmPassword}
              onChange={setConfirmPassword}
              autoComplete="new-password"
              maxLength={MAX_PASSWORD_LENGTH}
              sanitizeInput={sanitizeNewPasswordInput}
              passwordRuleHint={t('auth.password.rules')}
              className="admin-login__field"
              inputClassName="admin-login__input"
              leadingAdornment={<LockIcon />}
              iconOnlyToggle
            />

            {/* 固定狀態區避免錯誤訊息出現時推動主要按鈕，與登入入口保持同一個操作節奏。 */}
            <p className={`form-message admin-login__status ${statusTone}`} aria-live="polite">
              {error ?? '\u00a0'}
            </p>

            <button className="primary-button admin-login__submit" type="submit" disabled={loading}>
              {loading ? t('admin.auth.reset.submitting') : t('admin.auth.reset.submit')}
              <ArrowRightIcon />
            </button>
            <p className="admin-login__forgot-password">
              <NavLink to="/login">{t('admin.auth.forgot.backToLogin')}</NavLink>
            </p>
          </form>
          <p className="admin-login__security-notice"><ShieldIcon />{t('admin.auth.login.securityNotice')}</p>
        </Card>
      </main>
      <footer className="admin-login__footer">
        <p>{t('admin.auth.login.footerTagline')}</p>
        <p>{t('admin.auth.login.contactAdmin')}</p>
      </footer>
    </div>
  );
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
