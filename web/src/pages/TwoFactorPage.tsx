import { NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { useI18n } from '../i18n';

/** MFA 尚未完成 server-side runtime；頁面必須 fail closed，不能再用前端驗證碼假裝通過。 */
export function TwoFactorPage() {
  const { t } = useI18n();

  return (
    <AuthPageShell
      variant="twoFactor"
      footer={
        <p className="auth-page__helper">
          {t('auth.twoFactor.footerPrefix')} <NavLink to="/login">{t('auth.twoFactor.footerSignIn')}</NavLink>.
        </p>
      }
    >
      <section className="auth-form">
        <p className="form-message form-message--error">雙重驗證尚未啟用，系統不接受前端模擬驗證碼。</p>
        <p className="auth-form__hint">請使用帳號密碼登入；MFA 必須在伺服器端驗證與稽核完成後才會啟用。</p>
      </section>
    </AuthPageShell>
  );
}
