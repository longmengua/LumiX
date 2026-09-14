import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { PasswordField } from '../components/auth/PasswordField';
import { translateAuthError } from '../features/auth/authText';
import { resetPassword } from '../features/auth/authApi';
import { hasValidNewPassword, MAX_PASSWORD_LENGTH, sanitizeNewPasswordInput } from '../features/auth/passwordPolicy';
import { useI18n } from '../i18n';

export function ResetPasswordPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const token = searchParams.get('token') ?? '';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (!token) {
        throw new Error('AUTH_REQUEST_FAILED');
      }
      if (!hasValidNewPassword(newPassword)) {
        throw new Error('Password must be between 8 and 32 characters.');
      }
      if (newPassword !== confirmPassword) {
        throw new Error('Passwords do not match.');
      }
      await resetPassword({ token, newPassword });
      setSuccess(t('auth.reset.success'));
      navigate('/login', { replace: true });
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.reset.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      variant="reset"
      footer={
        <p className="auth-page__helper">
          {t('auth.reset.footerPrefix')} <NavLink to="/login">{t('auth.reset.footerSignIn')}</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="auth-form__split">
          <PasswordField
            label={t('auth.reset.newPassword')}
            value={newPassword}
            onChange={setNewPassword}
            placeholder={t('auth.reset.newPasswordPlaceholder')}
            autoComplete="new-password"
            maxLength={MAX_PASSWORD_LENGTH}
            sanitizeInput={sanitizeNewPasswordInput}
            passwordRuleHint={t('auth.password.rules')}
          />
          <PasswordField
            label={t('auth.reset.confirmPassword')}
            value={confirmPassword}
            onChange={setConfirmPassword}
            placeholder={t('auth.reset.confirmPasswordPlaceholder')}
            autoComplete="new-password"
            maxLength={MAX_PASSWORD_LENGTH}
            sanitizeInput={sanitizeNewPasswordInput}
            passwordRuleHint={t('auth.password.rules')}
          />
        </div>

        <p className="auth-form__hint">重設連結中的一次性 token 由伺服器驗證，使用後即失效。</p>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.reset.submitting') : t('auth.reset.submit')}
        </button>
      </form>

    </AuthPageShell>
  );
}
