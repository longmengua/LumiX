import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { verifyTwoFactorMock } from '../features/auth/mockAuthService';

export function TwoFactorPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const challenge = searchParams.get('challenge') ?? 'Demo verification challenge';
  const nextPath = searchParams.get('next') ?? '/';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await verifyTwoFactorMock(code);
      navigate(nextPath);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Two-factor verification failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow="Security"
      title="Two-factor verification"
      description={challenge}
      footer={
        <p className="auth-page__helper">
          Need a different account? <NavLink to="/login">Back to sign in</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">Verification code</span>
          <input
            className="input"
            inputMode="numeric"
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="123456"
          />
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Verifying...' : 'Verify and continue'}
        </button>

        <p className="auth-form__hint">Use 123456 in the demo flow.</p>
      </form>
    </AuthPageShell>
  );
}
