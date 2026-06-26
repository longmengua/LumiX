import { useState, type FormEvent } from 'react';
import { NavLink } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { requestPasswordResetMock } from '../features/auth/mockAuthService';

export function ForgotPasswordPage() {
  const [identifier, setIdentifier] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await requestPasswordResetMock(identifier);
      setSuccess(result);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to request reset.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow="Recovery"
      title="Reset your password"
      description="Request a reset link or code for the demo authentication flow."
      footer={
        <p className="auth-page__helper">
          Back to <NavLink to="/login">sign in</NavLink> or <NavLink to="/register">create account</NavLink>.
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
            autoComplete="username"
          />
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Sending...' : 'Send reset instructions'}
        </button>
      </form>
    </AuthPageShell>
  );
}
