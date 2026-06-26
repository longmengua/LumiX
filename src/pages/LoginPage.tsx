import { useState, type FormEvent } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { signInMock } from '../features/auth/mockAuthService';

export function LoginPage() {
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const result = await signInMock({ identifier, password, remember });

      if (result.status === 'two_factor') {
        navigate(`/two-factor?next=${encodeURIComponent(result.nextPath)}&challenge=${encodeURIComponent(result.challengeLabel)}`);
        return;
      }

      navigate(result.nextPath);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to sign in.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow="Phase 3"
      title="Sign in to LumiX"
      description="Access the exchange experience with a demo-ready authentication shell."
      footer={
        <p className="auth-page__helper">
          New here? <NavLink to="/register">Create an account</NavLink> or <NavLink to="/forgot-password">reset your password</NavLink>.
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">Email / phone</span>
          <input
            className="input"
            name="identifier"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder="name@example.com"
            autoComplete="username"
          />
        </label>

        <label className="field">
          <span className="field__label">Password</span>
          <input
            className="input"
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Enter password"
            autoComplete="current-password"
          />
        </label>

        <div className="auth-form__row">
          <label className="checkbox">
            <input checked={remember} type="checkbox" onChange={(event) => setRemember(event.target.checked)} />
            <span>Remember me</span>
          </label>
          <NavLink className="auth-form__link" to="/forgot-password">
            Forgot password?
          </NavLink>
        </div>

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Signing in...' : 'Sign in'}
        </button>

        <p className="auth-form__hint">Demo 2FA flow triggers when the identifier contains the word “secure”.</p>
      </form>
    </AuthPageShell>
  );
}
