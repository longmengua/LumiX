import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { registerMock } from '../features/auth/mockAuthService';

export function RegisterPage() {
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [referralCode, setReferralCode] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await registerMock({
        identifier,
        verificationCode,
        password,
        confirmPassword,
        referralCode: referralCode.trim() || undefined,
        acceptedTerms,
      });

      setSuccess(result);
      navigate('/login');
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to register.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow="Onboarding"
      title="Create your LumiX account"
      description="Use the Phase 3 onboarding shell to test validation, redirects, and security flows."
      footer={
        <p className="auth-page__helper">
          Already have an account? <NavLink to="/login">Sign in</NavLink>.
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

        <div className="auth-form__split">
          <label className="field">
            <span className="field__label">Verification code</span>
            <input
              className="input"
              value={verificationCode}
              onChange={(event) => setVerificationCode(event.target.value)}
              placeholder="123456"
            />
          </label>
          <label className="field">
            <span className="field__label">Referral code</span>
            <input
              className="input"
              value={referralCode}
              onChange={(event) => setReferralCode(event.target.value)}
              placeholder="Optional"
            />
          </label>
        </div>

        <div className="auth-form__split">
          <label className="field">
            <span className="field__label">Password</span>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
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

        <label className="checkbox">
          <input checked={acceptedTerms} type="checkbox" onChange={(event) => setAcceptedTerms(event.target.checked)} />
          <span>I agree to the terms and risk disclosures.</span>
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {success ? <p className="form-message form-message--success">{success}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Creating account...' : 'Create account'}
        </button>
      </form>
    </AuthPageShell>
  );
}
