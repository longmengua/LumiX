import { useState, type FormEvent } from 'react';
import { NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { SecurityVerifyModal, type VerificationMethod } from '../components/auth/SecurityVerifyModal';
import { resetPasswordMock } from '../features/auth/mockAuthService';

export function ResetPasswordPage() {
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
      const result = await resetPasswordMock({ identifier, newPassword, confirmPassword });
      setSuccess(result);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to reset password.');
    } finally {
      setLoading(false);
    }
  }

  function handleVerify(input: { method: VerificationMethod; code: string }) {
    if (input.code.trim() === '123456') {
      setVerifyOpen(false);
      setSuccess(`Verified by ${input.method}. You can now reset your password.`);
      return;
    }

    setError('Verification failed. Try 123456 in the demo flow.');
  }

  return (
    <AuthPageShell
      eyebrow="Recovery"
      title="Set a new password"
      description="Demo reset flow with a reusable security verification modal."
      footer={
        <p className="auth-page__helper">
          Return to <NavLink to="/login">sign in</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">Email / phone</span>
          <input
            className="input"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder="name@example.com"
          />
        </label>

        <div className="auth-form__split">
          <label className="field">
            <span className="field__label">New password</span>
            <input
              className="input"
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder="At least 8 characters"
              autoComplete="new-password"
            />
          </label>
          <label className="field">
            <span className="field__label">Confirm password</span>
            <input
              className="input"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder="Repeat password"
              autoComplete="new-password"
            />
          </label>
        </div>

        <div className="auth-form__row">
          <button className="secondary-button" type="button" onClick={() => setVerifyOpen(true)}>
            Open security verify
          </button>
          <span className="auth-form__hint">Modal is reusable for password changes, API key actions, and withdrawals.</span>
        </div>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Updating...' : 'Update password'}
        </button>
      </form>

      <SecurityVerifyModal
        confirmLabel="Confirm verification"
        description="Use any of the supported verification methods before changing a password."
        open={verifyOpen}
        title="Verify password reset"
        onClose={() => setVerifyOpen(false)}
        onConfirm={handleVerify}
      />
    </AuthPageShell>
  );
}
