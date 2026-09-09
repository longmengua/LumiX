import { useState, type FormEvent } from 'react';
import { Navigate, NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { PasswordField } from '../components/auth/PasswordField';
import { translateAuthError } from '../features/auth/authText';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { useI18n } from '../i18n';

/** 已登入使用者的改密碼頁面；伺服器會重新驗證舊密碼並撤銷所有舊 session。 */
export function ChangePasswordPage() {
  const { t } = useI18n();
  const { changePassword, sessionResolved, user } = useAuthentication();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      if (newPassword.length < 8 || newPassword.length > 32) {
        throw new Error('Password must be between 8 and 32 characters.');
      }
      if (newPassword !== confirmPassword) {
        throw new Error('Passwords do not match.');
      }
      await changePassword({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSuccess('密碼已更新，其他裝置的登入狀態已失效。');
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.reset.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  if (!sessionResolved) {
    return <AuthPageShell variant="reset"><p className="auth-form__hint">正在確認登入狀態…</p></AuthPageShell>;
  }

  if (!user) {
    return <Navigate replace to="/login" />;
  }

  return (
    <AuthPageShell
      variant="reset"
      footer={<p className="auth-page__helper"><NavLink to="/">返回首頁</NavLink></p>}
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <p className="auth-form__hint">目前帳號：{user.email}</p>
        <PasswordField label="目前密碼" value={currentPassword} onChange={setCurrentPassword} autoComplete="current-password" />
        <div className="auth-form__split">
          <PasswordField label="新密碼" value={newPassword} onChange={setNewPassword} autoComplete="new-password" />
          <PasswordField label="確認新密碼" value={confirmPassword} onChange={setConfirmPassword} autoComplete="new-password" />
        </div>
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}
        <button className="primary-button" type="submit" disabled={loading}>{loading ? '更新中…' : '更新密碼'}</button>
      </form>
    </AuthPageShell>
  );
}
