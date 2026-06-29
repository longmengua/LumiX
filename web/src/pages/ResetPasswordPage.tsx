import { useState, type FormEvent } from 'react';
import { NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { SecurityVerifyModal, type VerificationMethod } from '../components/auth/SecurityVerifyModal';
import { translateAuthError } from '../features/auth/authText';
import { resetPasswordMock } from '../features/auth/mockAuthService';
import { useI18n } from '../i18n';

export function ResetPasswordPage() {
  const { t } = useI18n();
  const [identifier, setIdentifier] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [verifyOpen, setVerifyOpen] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await resetPasswordMock({ identifier, newPassword, confirmPassword });
      setSuccess(t('auth.reset.success'));
    } catch (submitError) {
      setError(translateAuthError(submitError, t, 'auth.reset.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  function handleVerify(input: { method: VerificationMethod; code: string }) {
    if (input.code.trim() === '123456') {
      setVerifyOpen(false);
      setSuccess(t('auth.reset.verified', undefined, { method: t(`auth.verify.method.${input.method}`) }));
      return;
    }

    setError(t('auth.reset.verificationFailed'));
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
        <label className="field">
          <span className="field__label">{t('auth.reset.identifier')}</span>
          <input
            className="input"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder={t('auth.reset.identifierPlaceholder')}
          />
        </label>

        <div className="auth-form__split">
          <label className="field">
            <span className="field__label">{t('auth.reset.newPassword')}</span>
            <input
              className="input"
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder={t('auth.reset.newPasswordPlaceholder')}
              autoComplete="new-password"
            />
          </label>
          <label className="field">
            <span className="field__label">{t('auth.reset.confirmPassword')}</span>
            <input
              className="input"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder={t('auth.reset.confirmPasswordPlaceholder')}
              autoComplete="new-password"
            />
          </label>
        </div>

        <div className="auth-form__row">
          <button className="secondary-button" type="button" onClick={() => setVerifyOpen(true)}>
            {t('auth.reset.openVerify')}
          </button>
          <span className="auth-form__hint">{t('auth.reset.hint')}</span>
        </div>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? t('auth.reset.submitting') : t('auth.reset.submit')}
        </button>
      </form>

      <SecurityVerifyModal
        confirmLabel={t('auth.reset.verifyConfirm')}
        description={t('auth.reset.verifyDescription')}
        open={verifyOpen}
        title={t('auth.reset.verifyTitle')}
        onClose={() => setVerifyOpen(false)}
        onConfirm={handleVerify}
      />
    </AuthPageShell>
  );
}
