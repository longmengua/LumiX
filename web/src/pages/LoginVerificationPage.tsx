import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { AuthPageShell } from '../components/auth/AuthPageShell';
import { decideLoginVerification } from '../features/auth/authApi';
import { translateAuthError } from '../features/auth/authText';
import { useI18n } from '../i18n';

/**
 * 信件連結只載入這個確認畫面；真正的 Yes／No 由使用者明確送出 POST。
 *
 * token 永遠只送到同源 API，畫面不保存它、更不將它寫入 analytics 或 browser storage。
 */
export function LoginVerificationPage() {
  const { t } = useI18n();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<'APPROVED' | 'REJECTED' | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleDecision(event: { preventDefault(): void }, approved: boolean) {
    event.preventDefault();
    if (!token) {
      setError(t('auth.verification.invalidLink'));
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setResult(await decideLoginVerification(token, approved));
    } catch (decisionError) {
      setError(translateAuthError(decisionError, t, 'auth.verification.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPageShell variant="twoFactor" formAreaRatio={2}>
      <form className="auth-form" onSubmit={(event) => handleDecision(event, true)}>
        <div>
          <h1 className="auth-form__title">{t('auth.verification.title')}</h1>
          <p className="auth-form__hint">{t('auth.verification.description')}</p>
        </div>
        {result === 'APPROVED' ? <p className="form-message form-message--success">{t('auth.verification.approved')}</p> : null}
        {result === 'REJECTED' ? <p className="form-message form-message--success">{t('auth.verification.rejected')}</p> : null}
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {!result ? (
          <div className="auth-form__row">
            <button className="secondary-button" type="button" disabled={loading} onClick={(event) => void handleDecision(event, false)}>
              {t('auth.verification.no')}
            </button>
            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? t('auth.verification.submitting') : t('auth.verification.yes')}
            </button>
          </div>
        ) : null}
      </form>
    </AuthPageShell>
  );
}
